package com.syntilio.scaip.enums;

/**
 * SCAIP heartbeat options (hbo).
 */
public enum HeartbeatOptions {
    UNADJUSTABLE("0"),
    ADJUSTABLE("001");

    private final String code;

    HeartbeatOptions(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static HeartbeatOptions fromCode(String code) {
        if (code == null || code.isBlank()) return null;
        for (HeartbeatOptions h : values()) {
            if (h.code.equals(code)) return h;
        }
        return null;
    }
}
