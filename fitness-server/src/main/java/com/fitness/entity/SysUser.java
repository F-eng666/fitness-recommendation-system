package com.fitness.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;
    private String password;
    private String nickname;
    private String phone;
    private String email;
    private String avatar;

    private Integer gender;

    private Integer age;

    private BigDecimal height;

    private BigDecimal weight;

    private BigDecimal bmi;

    @TableField("fitness_goal")
    private String fitnessGoal;

    @TableField("exercise_frequency")
    private String exerciseFrequency;

    private String role;

    private Integer status;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
