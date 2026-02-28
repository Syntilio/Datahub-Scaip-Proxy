package com.syntilio.scaip;

import com.syntilio.scaip.enums.RequestTag;
import com.syntilio.scaip.enums.ResponseTag;
import com.syntilio.scaip.enums.StatusNumber;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * SCAIP XML message parsing and response building per SPEC.md (ScaipRequest/ScaipResponse).
 * Request uses short tags: ver, cid, dty, sco, cha, mty, hbo, did, dco, dte, crd, stc, stt, pri, lco, lva, lte, ico, ite, ame, ref.
 * Response uses: ref, snu (status_number 0=OK), ste (status_text), and optional cve, mre, cre, tnu, hbi.
 */
public final class ScaipXml {

    private static final String ROOT = "scaip";

    public static final String CONTENT_TYPE = "application/xml";

    /**
     * Result of parsing a SCAIP request per spec: either success with fields or failure with reason.
     */
    public static final class ParseResult {
        private final boolean ok;
        private final String reason;
        private final String ref;
        private final String ver;
        private final String controllerId;
        private final String deviceType;
        private final String systemConfig;
        private final String callHandling;
        private final String messageType;
        private final String heartbeatOptions;
        private final String deviceId;
        private final String deviceComponent;
        private final String deviceText;
        private final String callerId;
        private final String statusCode;
        private final String statusText;
        private final String priority;
        private final String locationCode;
        private final String locationValue;
        private final String locationText;
        private final String infoCode;
        private final String infoText;
        private final String additionalMessage;

        private ParseResult(boolean ok, String reason, String ref, String ver,
                            String controllerId, String deviceType, String systemConfig, String callHandling,
                            String messageType, String heartbeatOptions, String deviceId, String deviceComponent,
                            String deviceText, String callerId, String statusCode, String statusText,
                            String priority, String locationCode, String locationValue, String locationText,
                            String infoCode, String infoText, String additionalMessage) {
            this.ok = ok;
            this.reason = reason;
            this.ref = ref;
            this.ver = ver;
            this.controllerId = controllerId;
            this.deviceType = deviceType;
            this.systemConfig = systemConfig;
            this.callHandling = callHandling;
            this.messageType = messageType;
            this.heartbeatOptions = heartbeatOptions;
            this.deviceId = deviceId;
            this.deviceComponent = deviceComponent;
            this.deviceText = deviceText;
            this.callerId = callerId;
            this.statusCode = statusCode;
            this.statusText = statusText;
            this.priority = priority;
            this.locationCode = locationCode;
            this.locationValue = locationValue;
            this.locationText = locationText;
            this.infoCode = infoCode;
            this.infoText = infoText;
            this.additionalMessage = additionalMessage;
        }

        public static ParseResult success(String ref, String ver, String controllerId, String deviceType,
                                         String systemConfig, String callHandling, String messageType, String heartbeatOptions,
                                         String deviceId, String deviceComponent, String deviceText, String callerId,
                                         String statusCode, String statusText, String priority,
                                         String locationCode, String locationValue, String locationText,
                                         String infoCode, String infoText, String additionalMessage) {
            return new ParseResult(true, null, ref, ver, controllerId, deviceType, systemConfig, callHandling,
                messageType, heartbeatOptions, deviceId, deviceComponent, deviceText, callerId, statusCode, statusText,
                priority, locationCode, locationValue, locationText, infoCode, infoText, additionalMessage);
        }

        public static ParseResult failure(String reason) {
            return new ParseResult(false, reason, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        }

        public boolean isOk() { return ok; }
        public String getReason() { return reason; }
        public String getRef() { return ref; }
        public String getVer() { return ver; }
        public String getControllerId() { return controllerId; }
        public String getDeviceId() { return deviceId; }
        public String getDeviceType() { return deviceType; }
        public String getSystemConfig() { return systemConfig; }
        public String getCallHandling() { return callHandling; }
        public String getMessageType() { return messageType; }
        public String getHeartbeatOptions() { return heartbeatOptions; }
        public String getDeviceComponent() { return deviceComponent; }
        public String getDeviceText() { return deviceText; }
        public String getCallerId() { return callerId; }
        public String getStatusCode() { return statusCode; }
        public String getStatusText() { return statusText; }
        public String getPriority() { return priority; }
        public String getLocationCode() { return locationCode; }
        public String getLocationValue() { return locationValue; }
        public String getLocationText() { return locationText; }
        public String getInfoCode() { return infoCode; }
        public String getInfoText() { return infoText; }
        public String getAdditionalMessage() { return additionalMessage; }
    }

    /**
     * Parses a SCAIP request XML body (spec format with short tags). Returns ParseResult (success with fields or failure with reason).
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
            for (RequestTag tag : RequestTag.REQUIRED) {
                String v = getText(root, tag.getTagName());
                if (v == null || v.isBlank()) missing.add(tag.getTagName());
            }
            if (!missing.isEmpty()) {
                return ParseResult.failure("Missing required element(s): " + String.join(", ", missing));
            }
            String ref = getText(root, RequestTag.REF.getTagName());
            String ver = getText(root, RequestTag.VER.getTagName());
            String cid = getText(root, RequestTag.CID.getTagName());
            String dty = getText(root, RequestTag.DTY.getTagName());
            String sco = getText(root, RequestTag.SCO.getTagName());
            String cha = getText(root, RequestTag.CHA.getTagName());
            String mty = getText(root, RequestTag.MTY.getTagName());
            String hbo = getText(root, RequestTag.HBO.getTagName());
            String did = getText(root, RequestTag.DID.getTagName());
            String dco = getText(root, RequestTag.DCO.getTagName());
            String dte = getText(root, RequestTag.DTE.getTagName());
            String crd = getText(root, RequestTag.CRD.getTagName());
            String stc = getText(root, RequestTag.STC.getTagName());
            String stt = getText(root, RequestTag.STT.getTagName());
            String pri = getText(root, RequestTag.PRI.getTagName());
            String lco = getText(root, RequestTag.LCO.getTagName());
            String lva = getText(root, RequestTag.LVA.getTagName());
            String lte = getText(root, RequestTag.LTE.getTagName());
            String ico = getText(root, RequestTag.ICO.getTagName());
            String ite = getText(root, RequestTag.ITE.getTagName());
            String ame = getText(root, RequestTag.AME.getTagName());
            return ParseResult.success(ref, ver, cid, dty, sco, cha, mty, hbo, did, dco, dte, crd, stc, stt, pri, lco, lva, lte, ico, ite, ame);
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
     * Builds the ACK response body (spec format: ref, snu=0, ste).
     *
     * @param ref reference from the request (echoed back); if null a placeholder is used
     */
    public static String buildAck(String ref) {
        String r = ref != null && !ref.isEmpty() ? escape(ref) : "";
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<scaip>\n  <ref>" + r + "</ref>\n  <snu>" + StatusNumber.OK.getCode() + "</snu>\n  <ste></ste>\n</scaip>";
    }

    /**
     * Builds the NACK response body (spec format: ref, snu=status number, ste=status text).
     *
     * @param ref reference from the request; if null a placeholder is used
     * @param statusNumber status number (e.g. StatusNumber.MANDATORY_TAG_MISSING)
     * @param statusText reason text
     */
    public static String buildNack(String ref, StatusNumber statusNumber, String statusText) {
        String r = ref != null && !ref.isEmpty() ? escape(ref) : "";
        StatusNumber snu = statusNumber != null ? statusNumber : StatusNumber.MANDATORY_TAG_MISSING;
        String ste = statusText != null ? escape(statusText) : "";
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<scaip>\n  <ref>" + r + "</ref>\n  <snu>" + snu.getCode() + "</snu>\n  <ste>" + ste + "</ste>\n</scaip>";
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    /**
     * Parses a SCAIP response body. Returns "ACK" if snu=0, "NACK" otherwise, or null if not recognised.
     */
    public static String parseResponseResult(String xml) {
        String snu = parseResponseStatusNumber(xml);
        if (snu == null) return null;
        return StatusNumber.OK.getCode().equals(snu) ? "ACK" : "NACK";
    }

    /**
     * Parses status_number (snu) from response.
     */
    public static String parseResponseStatusNumber(String xml) {
        if (xml == null || xml.isBlank()) return null;
        try {
            Document doc = DocumentBuilderFactory.newDefaultInstance()
                .newDocumentBuilder()
                .parse(new ByteArrayInputStream(xml.trim().getBytes(StandardCharsets.UTF_8)));
            Element root = doc.getDocumentElement();
            if (root == null || !ROOT.equalsIgnoreCase(root.getTagName())) return null;
            return getText(root, ResponseTag.SNU.getTagName());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Gets the status_text (ste) from a response body, or null.
     */
    public static String parseResponseStatusText(String xml) {
        if (xml == null || xml.isBlank()) return null;
        try {
            Document doc = DocumentBuilderFactory.newDefaultInstance()
                .newDocumentBuilder()
                .parse(new ByteArrayInputStream(xml.trim().getBytes(StandardCharsets.UTF_8)));
            Element root = doc.getDocumentElement();
            if (root == null) return null;
            return getText(root, ResponseTag.STE.getTagName());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Gets the ref from a response body, or null.
     */
    public static String parseResponseRef(String xml) {
        if (xml == null || xml.isBlank()) return null;
        try {
            Document doc = DocumentBuilderFactory.newDefaultInstance()
                .newDocumentBuilder()
                .parse(new ByteArrayInputStream(xml.trim().getBytes(StandardCharsets.UTF_8)));
            Element root = doc.getDocumentElement();
            if (root == null) return null;
            return getText(root, ResponseTag.REF.getTagName());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Tries to extract ref from a request body (e.g. for echoing in NACK when full parse fails).
     */
    public static String getRefFromRequestBody(String xml) {
        if (xml == null || xml.isBlank()) return null;
        try {
            Document doc = DocumentBuilderFactory.newDefaultInstance()
                .newDocumentBuilder()
                .parse(new ByteArrayInputStream(xml.trim().getBytes(StandardCharsets.UTF_8)));
            Element root = doc.getDocumentElement();
            if (root == null) return null;
            return getText(root, RequestTag.REF.getTagName());
        } catch (Exception e) {
            return null;
        }
    }

    /** @deprecated Use {@link #parseResponseStatusText(String)} for spec response. */
    public static String parseResponseReason(String xml) {
        return parseResponseStatusText(xml);
    }
}
