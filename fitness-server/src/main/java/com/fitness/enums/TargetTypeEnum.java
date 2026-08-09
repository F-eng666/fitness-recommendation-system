package com.fitness.enums;

public enum TargetTypeEnum {
    ARTICLE("ARTICLE"),
    COURSE("COURSE"),
    VENUE("VENUE"),
    COACH("COACH");

    private final String code;

    TargetTypeEnum(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static TargetTypeEnum fromCode(String code) {
        for (TargetTypeEnum e : values()) {
            if (e.code.equals(code)) return e;
        }
        return ARTICLE;
    }
}