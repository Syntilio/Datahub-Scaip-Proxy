package com.syntilio.scaip.enums;

/**
 * SCAIP response status number (snu). Per spec: 0=OK, 1–7=error.
 */
public enum StatusNumber {
    OK("0"),
    MESSAGE_TOO_LONG("1"),
    INVALID_FORMAT("2"),
    WRONG_DATA_CONTENT("3"),
    HOLD("4"),
    NOT_TREATED("5"),
    BUSY("6"),
    MANDATORY_TAG_MISSING("7");

    private final String code;

    StatusNumber(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static StatusNumber fromCode(String code) {
        if (code == null || code.isBlank()) return null;
        for (StatusNumber n : values()) {
            if (n.code.equals(code)) return n;
        }
        return null;
    }
}
