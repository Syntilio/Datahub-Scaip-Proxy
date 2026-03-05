package com.syntilio.scaip.domain;

/**
 * SCAIP status code (stc). Common values; full list in SPEC.md.
 */
public enum StatusCode {
    UNKNOWN("0000"),
    UNDEFINED("0001"),
    MANUAL_ALARM("0010"),
    NORMAL_STATE("0070"),
    OCCUPIED("0071"),
    AUTOMATIC_RESET("0092"),
    MANUAL_RESET("0093");

    private final String code;

    StatusCode(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static StatusCode fromCode(String code) {
        if (code == null || code.isBlank()) return null;
        for (StatusCode s : values()) {
            if (s.code.equals(code)) return s;
        }
        return null;
    }
}
