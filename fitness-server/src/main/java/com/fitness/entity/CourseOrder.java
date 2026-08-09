package com.fitness.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("course_order")
public class CourseOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("order_no")
    private String orderNo;

    @TableField("user_id")
    private Long userId;

    @TableField("course_id")
    private Long courseId;

    private String status;

    @TableField("order_type")
    private String orderType;

    @TableField("pt_id")
    private Long ptId;

    @TableField("amount")
    private java.math.BigDecimal amount;

    private String remark;

    @TableField("refund_reason")
    private String refundReason;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
