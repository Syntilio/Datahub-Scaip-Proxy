package com.syntilio.scaip.domain;

/**
 * SCAIP call handling (cha).
 */
public enum CallHandling {
    OUTGOING_CALL("0"),
    CALLBACK("1");

    private final String code;

    CallHandling(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static CallHandling fromCode(String code) {
        if (code == null || code.isBlank()) return null;
        for (CallHandling c : values()) {
            if (c.code.equals(code)) return c;
        }
        return null;
    }
}
