package com.syntilio.scaip.domain;

/**
 * SCAIP device type (dty). Common values; full list in SPEC.md.
 */
public enum DeviceType {
    UNKNOWN("0000"),
    UNDEFINED("0001"),
    LOCAL_UNIT_AND_CONTROLLER("0002"),
    PERSONAL_TRIGGER("0003"),
    FIXED_TRIGGER("0004"),
    FALL_DETECTOR("0005"),
    PERSONAL_ATTACK_TRIGGER("0006"),
    PANIC_BUTTON("0007"),
    DUTY_SWITCH_REMOTE("0008"),
    DUTY_SWITCH_LOCAL("0009"),
    ACTIVITY_DETECTOR("0010"),
    PILL_DISPENSER("0011"),
    BED_MONITOR("0012"),
    MAT_SENSOR("0013"),
    DOOR_SENSOR("0014"),
    WANDERING_SENSOR("0015"),
    ENURESIS_DETECTOR("0016"),
    EPILEPSY_DETECTOR("0017"),
    OCCUPANCY_DETECTOR("0018"),
    HEART_RATE_MONITOR("0019"),
    CONTROLLER("0045"),
    LOCAL_UNIT("0048"),
    MONITORING_DEVICE("0049"),
    RADIO_UNIT("0050");

    private final String code;

    DeviceType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static DeviceType fromCode(String code) {
        if (code == null || code.isBlank()) return null;
        for (DeviceType d : values()) {
            if (d.code.equals(code)) return d;
        }
        return null;
    }
}
