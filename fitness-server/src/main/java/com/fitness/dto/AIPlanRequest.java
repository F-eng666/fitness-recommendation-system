package com.fitness.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;

@Data
public class AIPlanRequest {
    private Integer age;
    private Integer gender;
    private BigDecimal height;
    private BigDecimal weight;
    @NotBlank
    private String fitnessGoal;
    private String exerciseFrequency;
    private String availableEquipment;
    private String dietPreference;
    private String additionalNotes;
}