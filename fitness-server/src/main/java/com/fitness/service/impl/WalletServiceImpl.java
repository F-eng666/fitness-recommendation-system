package com.fitness.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fitness.common.BusinessException;
import com.fitness.common.PageResult;
import com.fitness.common.Result;
import com.fitness.entity.Wallet;
import com.fitness.entity.WalletTransaction;
import com.fitness.mapper.WalletMapper;
import com.fitness.mapper.WalletTransactionMapper;
import com.fitness.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletServiceImpl implements WalletService {

    private final WalletMapper walletMapper;
    private final WalletTransactionMapper walletTransactionMapper;

    @Override
    @Transactional
    public Result registerWallet(Long userId) {
        Wallet existing = walletMapper.selectByUserId(userId);
        if (existing == null) {
            Wallet wallet = new Wallet();
            wallet.setUserId(userId);
            wallet.setBalance(BigDecimal.ZERO);
            wallet.setCreateTime(LocalDateTime.now());
            wallet.setUpdateTime(LocalDateTime.now());
            walletMapper.insert(wallet);
            log.info("Created wallet for user {}", userId);
        }
        return Result.success("ok");
    }

    @Override
    public Result getMyWallet(Long userId) {
        Wallet wallet = walletMapper.selectByUserId(userId);
        if (wallet == null) {
            // auto-create wallet for existing users
            wallet = new Wallet();
            wallet.setUserId(userId);
            wallet.setBalance(BigDecimal.ZERO);
            wallet.setCreateTime(LocalDateTime.now());
            wallet.setUpdateTime(LocalDateTime.now());
            walletMapper.insert(wallet);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("balance", wallet.getBalance());
        result.put("userId", userId);
        return Result.success(result);
    }

    @Override
    public Result getTransactions(Long userId, Integer page, Integer size) {
        Page<WalletTransaction> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<WalletTransaction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WalletTransaction::getUserId, userId)
                .orderByDesc(WalletTransaction::getCreateTime);
        Page<WalletTransaction> pageResult = walletTransactionMapper.selectPage(pageParam, wrapper);
        return Result.success(PageResult.of(pageResult));
    }

    @Override
    @Transactional
    public Result recharge(Long userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("充值金额必须大于0");
        }
        Wallet wallet = walletMapper.selectByUserId(userId);
        if (wallet == null) {
            wallet = new Wallet();
            wallet.setUserId(userId);
            wallet.setBalance(BigDecimal.ZERO);
            wallet.setCreateTime(LocalDateTime.now());
            wallet.setUpdateTime(LocalDateTime.now());
            walletMapper.insert(wallet);
        }

        BigDecimal before = wallet.getBalance();
        BigDecimal after = before.add(amount);
        wallet.setBalance(after);
        wallet.setUpdateTime(LocalDateTime.now());
        walletMapper.updateById(wallet);

        WalletTransaction tx = new WalletTransaction();
        tx.setWalletId(wallet.getId());
        tx.setUserId(userId);
        tx.setType("RECHARGE");
        tx.setAmount(amount);
        tx.setBalanceBefore(before);
        tx.setBalanceAfter(after);
        tx.setRemark("余额充值");
        tx.setCreateTime(LocalDateTime.now());
        walletTransactionMapper.insert(tx);

        log.info("User {} recharged {}, balance {} -> {}", userId, amount, before, after);
        Map<String, Object> result = new HashMap<>();
        result.put("balance", after);
        result.put("rechargeAmount", amount);
        return Result.success(result);
    }
}
