package com.syntilio.scaip.enums;

/**
 * SCAIP response media reply (mre).
 */
public enum MediaReply {
    NO_VOICE_CALL("0"),
    DUPLEX_VOICE_CALL("1"),
    MICROPHONE_ONLY("2"),
    SPEAKER_ONLY("3");

    private final String code;

    MediaReply(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static MediaReply fromCode(String code) {
        if (code == null || code.isBlank()) return null;
        for (MediaReply m : values()) {
            if (m.code.equals(code)) return m;
        }
        return null;
    }
}
