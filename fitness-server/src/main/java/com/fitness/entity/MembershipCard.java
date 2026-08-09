package com.fitness.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("membership_card")
public class MembershipCard {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("venue_id")
    private Long venueId;

    private String name;

    @TableField("card_type")
    private String cardType;

    private BigDecimal price;

    @TableField("duration_days")
    private Integer durationDays;

    @TableField("session_count")
    private Integer sessionCount;

    private String description;

    private Integer status;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
