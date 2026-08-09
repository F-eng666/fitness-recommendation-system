package com.fitness.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("venue")
public class Venue {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String address;
    private String phone;
    private String description;
    private String images;

    @TableField("business_hours")
    private String businessHours;

    private String facilities;

    private BigDecimal longitude;

    private BigDecimal latitude;

    @TableField("admin_user_id")
    private Long adminUserId;

    private String status;

    private BigDecimal rating;

    @TableField("review_count")
    private Integer reviewCount;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
