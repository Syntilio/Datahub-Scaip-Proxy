package com.syntilio.scaip.domain;

/**
 * SCAIP message type (mty).
 */
public enum MessageType {
    MESSAGE("ME"),
    RESET("RE"),
    INFORMATION("IN"),
    HEARTBEAT("PI");

    private final String code;

    MessageType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static MessageType fromCode(String code) {
        if (code == null || code.isBlank()) return null;
        for (MessageType m : values()) {
            if (m.code.equals(code)) return m;
        }
        return null;
    }
}
