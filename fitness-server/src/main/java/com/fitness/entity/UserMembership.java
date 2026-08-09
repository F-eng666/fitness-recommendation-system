package com.fitness.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("user_membership")
public class UserMembership {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("card_id")
    private Long cardId;

    @TableField("venue_id")
    private Long venueId;

    @TableField("card_type")
    private String cardType;

    @TableField("order_no")
    private String orderNo;

    private BigDecimal price;

    @TableField("remaining_sessions")
    private Integer remainingSessions;

    @TableField("expire_time")
    private LocalDateTime expireTime;

    private String status;

    @TableField("create_time")
    private LocalDateTime createTime;
}
