package com.syntilio.scaip;

import com.syntilio.scaip.enums.StatusNumber;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ScaipXmlTest {

    private static final String MINIMAL_VALID_REQUEST =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<scaip>\n"
            + "  <ref>req-123</ref>\n"
            + "  <ver>1</ver>\n"
            + "  <cid>controller-1</cid>\n"
            + "  <dty>device-type-a</dty>\n"
            + "  <sco></sco><cha></cha><mty></mty><hbo></hbo>\n"
            + "  <did>dev-1</did><dco></dco><dte></dte><crd></crd>\n"
            + "  <stc>stc1</stc><stt></stt><pri>1</pri>\n"
            + "  <lco>lco1</lco><lva></lva><lte>loc text</lte>\n"
            + "  <ico></ico><ite></ite><ame></ame>\n"
            + "</scaip>";

    @Nested
    class ParseRequest {
        @Test
        void returnsFailureForNull() {
            ScaipXml.ParseResult result = ScaipXml.parseRequest(null);
            assertFalse(result.isOk());
            assertEquals("Empty body", result.getReason());
        }

        @Test
        void returnsFailureForBlank() {
            assertFalse(ScaipXml.parseRequest("").isOk());
            assertFalse(ScaipXml.parseRequest("   ").isOk());
            assertTrue(ScaipXml.parseRequest("   ").getReason().contains("Empty"));
        }

        @Test
        void returnsFailureForInvalidXml() {
            ScaipXml.ParseResult result = ScaipXml.parseRequest("<not-xml");
            assertFalse(result.isOk());
            assertTrue(result.getReason().startsWith("Invalid XML:"));
        }

        @Test
        void returnsFailureWhenRootIsNotScaip() {
            String xml = "<?xml version=\"1.0\"?><other><cid>x</cid><dty>y</dty></other>";
            ScaipXml.ParseResult result = ScaipXml.parseRequest(xml);
            assertFalse(result.isOk());
            assertEquals("Root element must be <scaip>", result.getReason());
        }

        @Test
        void returnsFailureWhenRequiredTagMissing() {
            String xml = "<?xml version=\"1.0\"?><scaip><ref>r</ref><cid>c</cid></scaip>";
            ScaipXml.ParseResult result = ScaipXml.parseRequest(xml);
            assertFalse(result.isOk());
            assertTrue(result.getReason().contains("dty"));
        }

        @Test
        void returnsFailureWhenCidMissing() {
            String xml = "<?xml version=\"1.0\"?><scaip><ref>r</ref><dty>d</dty></scaip>";
            ScaipXml.ParseResult result = ScaipXml.parseRequest(xml);
            assertFalse(result.isOk());
            assertTrue(result.getReason().contains("cid"));
        }

        @Test
        void returnsSuccessForMinimalValidRequest() {
            ScaipXml.ParseResult result = ScaipXml.parseRequest(MINIMAL_VALID_REQUEST);
            assertTrue(result.isOk());
            assertEquals("req-123", result.getRef());
            assertEquals("1", result.getVer());
            assertEquals("controller-1", result.getControllerId());
            assertEquals("device-type-a", result.getDeviceType());
            assertEquals("dev-1", result.getDeviceId());
            assertEquals("stc1", result.getStatusCode());
            assertEquals("1", result.getPriority());
            assertEquals("lco1", result.getLocationCode());
            assertEquals("loc text", result.getLocationText());
        }

        @Test
        void rootTagNameIsCaseInsensitive() {
            String xml = "<?xml version=\"1.0\"?><SCAIP><cid>c</cid><dty>d</dty></SCAIP>";
            ScaipXml.ParseResult result = ScaipXml.parseRequest(xml);
            assertTrue(result.isOk());
        }
    }

    @Nested
    class BuildAck {
        @Test
        void containsRefAndSnuZero() {
            String xml = ScaipXml.buildAck("my-ref");
            assertTrue(xml.contains("<ref>my-ref</ref>"));
            assertTrue(xml.contains("<snu>0</snu>"));
            assertTrue(xml.contains("<scaip>"));
        }

        @Test
        void acceptsNullRef() {
            String xml = ScaipXml.buildAck(null);
            assertTrue(xml.contains("<ref></ref>"));
            assertTrue(xml.contains("<snu>0</snu>"));
        }

        @Test
        void acceptsEmptyRef() {
            String xml = ScaipXml.buildAck("");
            assertTrue(xml.contains("<ref></ref>"));
        }

        @Test
        void escapesRefContent() {
            String xml = ScaipXml.buildAck("a<b&c>");
            assertTrue(xml.contains("&lt;"));
            assertTrue(xml.contains("&amp;"));
            assertTrue(xml.contains("&gt;"));
        }
    }

    @Nested
    class BuildNack {
        @Test
        void containsRefStatusNumberAndStatusText() {
            String xml = ScaipXml.buildNack("ref-1", StatusNumber.MANDATORY_TAG_MISSING, "Missing cid");
            assertTrue(xml.contains("<ref>ref-1</ref>"));
            assertTrue(xml.contains("<snu>7</snu>"));
            assertTrue(xml.contains("<ste>Missing cid</ste>"));
        }

        @Test
        void usesCorrectCodeForInvalidFormat() {
            String xml = ScaipXml.buildNack("r", StatusNumber.INVALID_FORMAT, "bad");
            assertTrue(xml.contains("<snu>2</snu>"));
        }

        @Test
        void acceptsNullRef() {
            String xml = ScaipXml.buildNack(null, StatusNumber.OK, "msg");
            assertTrue(xml.contains("<ref></ref>"));
        }

        @Test
        void acceptsNullStatusNumberDefaultsToMandatoryTagMissing() {
            String xml = ScaipXml.buildNack("r", null, "reason");
            assertTrue(xml.contains("<snu>7</snu>"));
        }

        @Test
        void escapesStatusText() {
            String xml = ScaipXml.buildNack("r", StatusNumber.INVALID_FORMAT, "error <tag>");
            assertTrue(xml.contains("&lt;"));
            assertTrue(xml.contains("&gt;"));
        }
    }

    @Nested
    class ParseResponse {
        private static final String ACK_RESPONSE =
            "<?xml version=\"1.0\"?><scaip><ref>r1</ref><snu>0</snu><ste>OK</ste></scaip>";
        private static final String NACK_RESPONSE =
            "<?xml version=\"1.0\"?><scaip><ref>r2</ref><snu>7</snu><ste>Missing tag</ste></scaip>";

        @Test
        void parseResponseResultReturnsAckWhenSnuZero() {
            assertEquals("ACK", ScaipXml.parseResponseResult(ACK_RESPONSE));
        }

        @Test
        void parseResponseResultReturnsNackWhenSnuNonZero() {
            assertEquals("NACK", ScaipXml.parseResponseResult(NACK_RESPONSE));
        }

        @Test
        void parseResponseResultReturnsNullForInvalidXml() {
            assertNull(ScaipXml.parseResponseResult(null));
            assertNull(ScaipXml.parseResponseResult(""));
            assertNull(ScaipXml.parseResponseResult("<other></other>"));
        }

        @Test
        void parseResponseStatusNumber() {
            assertEquals("0", ScaipXml.parseResponseStatusNumber(ACK_RESPONSE));
            assertEquals("7", ScaipXml.parseResponseStatusNumber(NACK_RESPONSE));
            assertNull(ScaipXml.parseResponseStatusNumber(""));
            assertNull(ScaipXml.parseResponseStatusNumber("<scaip></scaip>"));
        }

        @Test
        void parseResponseStatusText() {
            assertEquals("OK", ScaipXml.parseResponseStatusText(ACK_RESPONSE));
            assertEquals("Missing tag", ScaipXml.parseResponseStatusText(NACK_RESPONSE));
            assertNull(ScaipXml.parseResponseStatusText(null));
        }

        @Test
        void parseResponseRef() {
            assertEquals("r1", ScaipXml.parseResponseRef(ACK_RESPONSE));
            assertEquals("r2", ScaipXml.parseResponseRef(NACK_RESPONSE));
            assertNull(ScaipXml.parseResponseRef("  "));
        }
    }

    @Nested
    class GetRefFromRequestBody {
        @Test
        void extractsRefFromValidRequest() {
            assertEquals("req-123", ScaipXml.getRefFromRequestBody(MINIMAL_VALID_REQUEST));
        }

        @Test
        void returnsNullForNullOrBlank() {
            assertNull(ScaipXml.getRefFromRequestBody(null));
            assertNull(ScaipXml.getRefFromRequestBody(""));
        }

        @Test
        void returnsNullForInvalidXml() {
            assertNull(ScaipXml.getRefFromRequestBody("<broken"));
        }

        @Test
        void returnsNullWhenNoRefElement() {
            String xml = "<?xml version=\"1.0\"?><scaip><cid>c</cid><dty>d</dty></scaip>";
            assertNull(ScaipXml.getRefFromRequestBody(xml));
        }
    }

    @Nested
    class ParseResult {
        @Test
        void successHasAllFields() {
            ScaipXml.ParseResult r = ScaipXml.ParseResult.success(
                "ref", "ver", "cid", "dty", "sco", "cha", "mty", "hbo",
                "did", "dco", "dte", "crd", "stc", "stt", "pri",
                "lco", "lva", "lte", "ico", "ite", "ame");
            assertTrue(r.isOk());
            assertNull(r.getReason());
            assertEquals("ref", r.getRef());
            assertEquals("cid", r.getControllerId());
            assertEquals("dty", r.getDeviceType());
        }

        @Test
        void failureHasReasonOnly() {
            ScaipXml.ParseResult r = ScaipXml.ParseResult.failure("something wrong");
            assertFalse(r.isOk());
            assertEquals("something wrong", r.getReason());
            assertNull(r.getRef());
        }
    }
}
