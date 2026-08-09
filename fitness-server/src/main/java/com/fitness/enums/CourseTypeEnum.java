package com.fitness.enums;
public enum CourseTypeEnum {
    STRENGTH("STRENGTH"), YOGA("YOGA"), BOXING("BOXING"), SPINNING("SPINNING"), PILATES("PILATES"), HIIT("HIIT");
    private final String code;
    CourseTypeEnum(String code) { this.code = code; }
    public String getCode() { return code; }
}