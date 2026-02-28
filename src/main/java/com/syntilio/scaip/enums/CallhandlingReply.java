package com.syntilio.scaip.enums;

/**
 * SCAIP response callhandling reply (cre).
 */
public enum CallhandlingReply {
    PRE_DEFINED_RECEIVER("61"),
    TRANSFERRED_NUMBER("62");

    private final String code;

    CallhandlingReply(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static CallhandlingReply fromCode(String code) {
        if (code == null || code.isBlank()) return null;
        for (CallhandlingReply c : values()) {
            if (c.code.equals(code)) return c;
        }
        return null;
    }
}
