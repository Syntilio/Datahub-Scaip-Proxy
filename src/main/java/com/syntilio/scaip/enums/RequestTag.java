package com.syntilio.scaip.enums;

import java.util.List;

/**
 * SCAIP request XML tag names (short names per SPEC.md).
 */
public enum RequestTag {
    VER("ver", "version"),
    CID("cid", "controller_id"),
    DTY("dty", "device_type"),
    SCO("sco", "system_config"),
    CHA("cha", "call_handling"),
    MTY("mty", "message_type"),
    HBO("hbo", "heartbeat_options"),
    DID("did", "device_id"),
    DCO("dco", "device_component"),
    DTE("dte", "device_text"),
    CRD("crd", "caller_id"),
    STC("stc", "status_code"),
    STT("stt", "status_text"),
    PRI("pri", "priority"),
    LCO("lco", "location_code"),
    LVA("lva", "location_value"),
    LTE("lte", "location_text"),
    ICO("ico", "info_code"),
    ITE("ite", "info_text"),
    AME("ame", "additional_message"),
    REF("ref", "reference");

    private final String tagName;
    private final String fullName;

    RequestTag(String tagName, String fullName) {
        this.tagName = tagName;
        this.fullName = fullName;
    }

    public String getTagName() {
        return tagName;
    }

    public String getFullName() {
        return fullName;
    }

    /** Required request tags (cid, dty). */
    public static final List<RequestTag> REQUIRED = List.of(CID, DTY);
}
