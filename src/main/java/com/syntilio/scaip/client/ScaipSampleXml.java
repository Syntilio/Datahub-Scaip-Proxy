package com.syntilio.scaip.client;

/**
 * Hardcoded SCAIP request XML samples (no POJO). Use REF_PLACEHOLDER in alarm/heartbeat
 * and replace with actual ref before sending.
 */
public final class ScaipSampleXml {

    private static final String REF_PLACEHOLDER = "REF_PLACEHOLDER";

    /** Valid alarm (cid, dty, did, dco, stc, lco, lte, pri, ref). */
    public static final String ALARM =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<scaip>\n"
            + "  <ver>01.00</ver>\n"
            + "  <cid>+123456</cid>\n"
            + "  <dty>0004</dty>\n"
            + "  <did>001d940cb800</did>\n"
            + "  <dco>007</dco>\n"
            + "  <stc>0010</stc>\n"
            + "  <pri>0</pri>\n"
            + "  <lco>021</lco>\n"
            + "  <lte>kitchen</lte>\n"
            + "  <ref>" + REF_PLACEHOLDER + "</ref>\n"
            + "</scaip>";

    /** Valid heartbeat (cid, dty, did, stc, ref). */
    public static final String HEARTBEAT =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<scaip>\n"
            + "  <ver>01.00</ver>\n"
            + "  <cid>+123456</cid>\n"
            + "  <dty>0004</dty>\n"
            + "  <did>001d940cb800</did>\n"
            + "  <stc>0070</stc>\n"
            + "  <ref>" + REF_PLACEHOLDER + "</ref>\n"
            + "</scaip>";

    /** Invalid request (missing required dty) -> expect NACK. */
    public static final String INVALID_MISSING_DTY =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<scaip>\n"
            + "  <ver>01.00</ver>\n"
            + "  <cid>+123456</cid>\n"
            + "  <did>001d940cb800</did>\n"
            + "  <stc>0010</stc>\n"
            + "</scaip>";

    /** Invalid XML (missing closing tag) but with valid device ID -> expect NACK (Invalid XML). */
    public static final String INVALID_MALFORMED_XML =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<scaip>\n"
            + "  <ver>01.00</ver>\n"
            + "  <cid>+123456</cid>\n"
            + "  <dty>0004</dty>\n"
            + "  <did>001d940cb800</did>\n"
            + "  <stc>0070</stc>\n"
            + "  <ref>ref-malformed</ref>\n";

    /** Returns alarm XML with ref substituted. */
    public static String alarmWithRef(String ref) {
        return ALARM.replace(REF_PLACEHOLDER, ref != null ? ref : "");
    }

    /** Returns heartbeat XML with ref substituted. */
    public static String heartbeatWithRef(String ref) {
        return HEARTBEAT.replace(REF_PLACEHOLDER, ref != null ? ref : "");
    }

    private ScaipSampleXml() {}
}
