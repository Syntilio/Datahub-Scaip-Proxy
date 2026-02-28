package com.syntilio.scaip;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * SCAIP XML message parsing and ACK/NACK response building.
 * See SCAIP_PROTOCOL.md for field definitions.
 */
public final class ScaipXml {

    private static final String ROOT = "scaip";
    private static final String RESULT = "result";
    private static final String REASON = "reason";
    private static final String ACK = "ACK";
    private static final String NACK = "NACK";

    public static final String CONTENT_TYPE = "application/xml";

    /** Required element names for a valid SCAIP event. */
    private static final String[] REQUIRED = {"controllerId", "deviceId", "deviceType", "statusCode"};

    /**
     * Result of parsing a SCAIP request: either success with fields or failure with reason.
     */
    public static final class ParseResult {
        private final boolean ok;
        private final String reason;
        private final String controllerId;
        private final String deviceId;
        private final String deviceType;
        private final String statusCode;
        private final String deviceComponent;
        private final String location;
        private final String priority;

        private ParseResult(boolean ok, String reason,
                           String controllerId, String deviceId, String deviceType, String statusCode,
                           String deviceComponent, String location, String priority) {
            this.ok = ok;
            this.reason = reason;
            this.controllerId = controllerId;
            this.deviceId = deviceId;
            this.deviceType = deviceType;
            this.statusCode = statusCode;
            this.deviceComponent = deviceComponent;
            this.location = location;
            this.priority = priority;
        }

        public static ParseResult success(String controllerId, String deviceId, String deviceType, String statusCode,
                                         String deviceComponent, String location, String priority) {
            return new ParseResult(true, null, controllerId, deviceId, deviceType, statusCode,
                deviceComponent, location, priority);
        }

        public static ParseResult failure(String reason) {
            return new ParseResult(false, reason, null, null, null, null, null, null, null);
        }

        public boolean isOk() { return ok; }
        public String getReason() { return reason; }
        public String getControllerId() { return controllerId; }
        public String getDeviceId() { return deviceId; }
        public String getDeviceType() { return deviceType; }
        public String getStatusCode() { return statusCode; }
        public String getDeviceComponent() { return deviceComponent; }
        public String getLocation() { return location; }
        public String getPriority() { return priority; }
    }

    /**
     * Parses a SCAIP request XML body. Returns a ParseResult (success with fields or failure with reason).
     */
    public static ParseResult parseRequest(String xml) {
        if (xml == null || xml.isBlank()) {
            return ParseResult.failure("Empty body");
        }
        try {
            Document doc = DocumentBuilderFactory.newDefaultInstance()
                .newDocumentBuilder()
                .parse(new ByteArrayInputStream(xml.trim().getBytes(StandardCharsets.UTF_8)));
            Element root = doc.getDocumentElement();
            if (root == null || !ROOT.equalsIgnoreCase(root.getTagName())) {
                return ParseResult.failure("Root element must be <scaip>");
            }
            List<String> missing = new ArrayList<>();
            String controllerId = getText(root, "controllerId");
            String deviceId = getText(root, "deviceId");
            String deviceType = getText(root, "deviceType");
            String statusCode = getText(root, "statusCode");
            for (String tag : REQUIRED) {
                String v = getText(root, tag);
                if (v == null || v.isBlank()) missing.add(tag);
            }
            if (!missing.isEmpty()) {
                return ParseResult.failure("Missing required element(s): " + String.join(", ", missing));
            }
            String deviceComponent = getText(root, "deviceComponent");
            String location = getText(root, "location");
            String priority = getText(root, "priority");
            return ParseResult.success(controllerId, deviceId, deviceType, statusCode, deviceComponent, location, priority);
        } catch (Exception e) {
            return ParseResult.failure("Invalid XML: " + e.getMessage());
        }
    }

    private static String getText(Element parent, String tagName) {
        NodeList list = parent.getElementsByTagName(tagName);
        if (list.getLength() == 0) return null;
        String t = list.item(0).getTextContent();
        return t != null ? t.trim() : null;
    }

    /**
     * Builds the ACK response body (XML).
     */
    public static String buildAck() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<scaip>\n  <result>ACK</result>\n</scaip>";
    }

    /**
     * Builds the NACK response body (XML) with optional reason.
     */
    public static String buildNack(String reason) {
        String r = (reason != null && !reason.isEmpty()) ? reason : "Unknown error";
        r = r.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<scaip>\n  <result>NACK</result>\n  <reason>" + r + "</reason>\n</scaip>";
    }

    /**
     * Parses a SCAIP response body (ACK/NACK). Returns "ACK", "NACK", or null if not recognised.
     */
    public static String parseResponseResult(String xml) {
        if (xml == null || xml.isBlank()) return null;
        try {
            Document doc = DocumentBuilderFactory.newDefaultInstance()
                .newDocumentBuilder()
                .parse(new ByteArrayInputStream(xml.trim().getBytes(StandardCharsets.UTF_8)));
            Element root = doc.getDocumentElement();
            if (root == null || !ROOT.equalsIgnoreCase(root.getTagName())) return null;
            String result = getText(root, RESULT);
            if (ACK.equalsIgnoreCase(result)) return "ACK";
            if (NACK.equalsIgnoreCase(result)) return "NACK";
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Gets the reason from a NACK response body, or null.
     */
    public static String parseResponseReason(String xml) {
        if (xml == null || xml.isBlank()) return null;
        try {
            Document doc = DocumentBuilderFactory.newDefaultInstance()
                .newDocumentBuilder()
                .parse(new ByteArrayInputStream(xml.trim().getBytes(StandardCharsets.UTF_8)));
            Element root = doc.getDocumentElement();
            if (root == null) return null;
            return getText(root, REASON);
        } catch (Exception e) {
            return null;
        }
    }
}
