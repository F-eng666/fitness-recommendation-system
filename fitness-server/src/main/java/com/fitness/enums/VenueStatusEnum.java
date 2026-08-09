package com.fitness.enums;
public enum VenueStatusEnum {
    PENDING("PENDING"), APPROVED("APPROVED"), REJECTED("REJECTED");
    private final String code;
    VenueStatusEnum(String code) { this.code = code; }
    public String getCode() { return code; }
}