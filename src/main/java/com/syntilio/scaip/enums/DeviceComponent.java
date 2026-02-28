package com.syntilio.scaip.enums;

/**
 * SCAIP device component (dco). Common values; full list in SPEC.md.
 */
public enum DeviceComponent {
    UNKNOWN("000"),
    UNDEFINED("001"),
    BUTTON_1("002"),
    BUTTON_2("003"),
    BUTTON_3("004"),
    BUTTON_4("005"),
    BUTTON_5("006"),
    SWITCH_1("007"),
    SWITCH_2("008"),
    SWITCH_3("009"),
    SWITCH_4("010"),
    SWITCH_5("011");

    private final String code;

    DeviceComponent(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static DeviceComponent fromCode(String code) {
        if (code == null || code.isBlank()) return null;
        for (DeviceComponent d : values()) {
            if (d.code.equals(code)) return d;
        }
        return null;
    }
}
