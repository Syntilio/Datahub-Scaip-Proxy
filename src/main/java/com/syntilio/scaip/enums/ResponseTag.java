package com.syntilio.scaip.enums;

/**
 * SCAIP response XML tag names (short names per SPEC.md).
 */
public enum ResponseTag {
    REF("ref", "reference"),
    SNU("snu", "status_number"),
    STE("ste", "status_text"),
    CVE("cve", "common_version"),
    MRE("mre", "media_reply"),
    CRE("cre", "callhandling_reply"),
    TNU("tnu", "transferred_number"),
    HBI("hbi", "heartbeat_interval");

    private final String tagName;
    private final String fullName;

    ResponseTag(String tagName, String fullName) {
        this.tagName = tagName;
        this.fullName = fullName;
    }

    public String getTagName() {
        return tagName;
    }

    public String getFullName() {
        return fullName;
    }
}
