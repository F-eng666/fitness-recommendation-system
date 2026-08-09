package com.fitness.enums;

public enum OrderStatusEnum {
    PENDING("PENDING"),
    CONFIRMED("CONFIRMED"),
    COMPLETED("COMPLETED"),
    CANCELLED("CANCELLED");

    private final String code;

    OrderStatusEnum(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static OrderStatusEnum fromCode(String code) {
        for (OrderStatusEnum e : values()) {
            if (e.code.equals(code)) return e;
        }
        return PENDING;
    }
}