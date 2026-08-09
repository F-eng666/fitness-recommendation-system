package com.fitness.enums;

public enum TagTypeEnum {
    SKILL("SKILL"), INTEREST("INTEREST"), GOAL("GOAL"), BODY("BODY"), BEHAVIOR("BEHAVIOR");
    private final String code;
    TagTypeEnum(String code) { this.code = code; }
    public String getCode() { return code; }
    public static TagTypeEnum fromCode(String code) {
        for (TagTypeEnum e : values()) {
            if (e.code.equals(code)) return e;
        }
        return SKILL;
    }
}