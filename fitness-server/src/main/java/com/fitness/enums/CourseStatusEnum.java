package com.fitness.enums;
public enum CourseStatusEnum {
    UPCOMING("UPCOMING"), ONGOING("ONGOING"), FINISHED("FINISHED"), CANCELLED("CANCELLED");
    private final String code;
    CourseStatusEnum(String code) { this.code = code; }
    public String getCode() { return code; }
    public static CourseStatusEnum fromCode(String code) {
        for (CourseStatusEnum e : values()) { if (e.code.equals(code)) return e; }
        return UPCOMING;
    }
}