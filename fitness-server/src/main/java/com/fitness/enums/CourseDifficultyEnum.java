package com.fitness.enums;
public enum CourseDifficultyEnum {
    BEGINNER("BEGINNER"), INTERMEDIATE("INTERMEDIATE"), ADVANCED("ADVANCED");
    private final String code;
    CourseDifficultyEnum(String code) { this.code = code; }
    public String getCode() { return code; }
}