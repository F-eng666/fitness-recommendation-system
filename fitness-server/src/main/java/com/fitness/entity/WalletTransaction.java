package com.fitness.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("wallet_transaction")
public class WalletTransaction {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("wallet_id")
    private Long walletId;

    @TableField("user_id")
    private Long userId;

    private String type;

    private BigDecimal amount;

    @TableField("balance_before")
    private BigDecimal balanceBefore;

    @TableField("balance_after")
    private BigDecimal balanceAfter;

    @TableField("order_id")
    private Long orderId;

    private String remark;

    @TableField("create_time")
    private LocalDateTime createTime;
}
