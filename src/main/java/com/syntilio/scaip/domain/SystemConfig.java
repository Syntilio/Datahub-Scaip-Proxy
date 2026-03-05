package com.syntilio.scaip.domain;

/**
 * SCAIP system config (sco).
 */
public enum SystemConfig {
    LOCAL_UNIT_AND_CONTROLLER("0"),
    GROUPED_EQUIPMENT_WITH_SUPERVISOR_OFF_DUTY("1"),
    GROUPED_EQUIPMENT_WITH_SUPERVISOR_ON_DUTY("2"),
    GROUPED_EQUIPMENT_WITH_SUPERVISOR_ON_DUTY_ACTING_AS_ALARM_RECEIVER("3");

    private final String code;

    SystemConfig(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static SystemConfig fromCode(String code) {
        if (code == null || code.isBlank()) return null;
        for (SystemConfig s : values()) {
            if (s.code.equals(code)) return s;
        }
        return null;
    }
}
