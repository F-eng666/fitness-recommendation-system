package com.fitness.service;

import com.fitness.common.Result;

public interface WalletService {
    Result getMyWallet(Long userId);
    Result getTransactions(Long userId, Integer page, Integer size);
    Result recharge(Long userId, java.math.BigDecimal amount);
    Result registerWallet(Long userId);
}
