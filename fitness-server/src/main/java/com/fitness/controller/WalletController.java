package com.fitness.controller;

import com.fitness.common.BusinessException;
import com.fitness.common.Result;
import com.fitness.mapper.SysUserMapper;
import com.fitness.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.math.BigDecimal;

@Slf4j
@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @Resource
    private SysUserMapper sysUserMapper;

    @GetMapping("/my")
    public Result myWallet() {
        Long userId = getCurrentUserId();
        log.info("Get wallet: userId={}", userId);
        return walletService.getMyWallet(userId);
    }

    @GetMapping("/transactions")
    public Result transactions(@RequestParam(defaultValue = "1") Integer page,
                                @RequestParam(defaultValue = "20") Integer size) {
        Long userId = getCurrentUserId();
        log.info("Get wallet transactions: userId={}, page={}, size={}", userId, page, size);
        return walletService.getTransactions(userId, page, size);
    }

    @PostMapping("/recharge")
    public Result recharge(@RequestParam BigDecimal amount) {
        Long userId = getCurrentUserId();
        log.info("Wallet recharge: userId={}, amount={}", userId, amount);
        return walletService.recharge(userId, amount);
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new BusinessException(401, "请先登录");
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof String) {
            String str = (String) principal;
            try {
                return Long.valueOf(str);
            } catch (NumberFormatException e) {
                var user = sysUserMapper.selectByUsername(str);
                if (user != null) {
                    return user.getId();
                }
            }
        }
        throw new BusinessException(401, "无法获取用户信息");
    }
}
