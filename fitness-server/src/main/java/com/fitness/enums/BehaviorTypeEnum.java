package com.fitness.enums;

public enum BehaviorTypeEnum {

    BROWSE("BROWSE", "BROWSE"),
    BOOK("BOOK", "BOOK"),
    LIKE("LIKE", "LIKE"),
    COMMENT("COMMENT", "COMMENT"),
    CHECK_IN("CHECK_IN", "CHECK_IN"),
    FAVORITE("FAVORITE", "FAVORITE");

    private final String code;
    private final String description;

    BehaviorTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() { return code; }
    public String getDescription() { return description; }

    public static BehaviorTypeEnum fromCode(String code) {
        for (BehaviorTypeEnum e : values()) {
            if (e.code.equals(code)) return e;
        }
        return BROWSE;
    }
}
