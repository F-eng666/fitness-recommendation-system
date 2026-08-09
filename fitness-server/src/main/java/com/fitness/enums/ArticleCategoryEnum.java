package com.fitness.enums;
public enum ArticleCategoryEnum {
    BEGINNER("BEGINNER"), NUTRITION("NUTRITION"), TECHNIQUE("TECHNIQUE"), TRAINING("TRAINING");
    private final String code;
    ArticleCategoryEnum(String code) { this.code = code; }
    public String getCode() { return code; }
}