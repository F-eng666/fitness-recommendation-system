package com.fitness.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class UserProfileRequest {
    private String nickname;
    private String phone;
    private String email;
    private String avatar;
    private Integer gender;
    private Integer age;
    private BigDecimal height;
    private BigDecimal weight;
    private String fitnessGoal;
    private String exerciseFrequency;
}