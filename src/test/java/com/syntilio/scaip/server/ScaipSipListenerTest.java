package com.syntilio.scaip.server;

import org.junit.jupiter.api.Test;

import com.syntilio.scaip.domain.ScaipXml;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the response body logic in ScaipSipListener (buildResponseBody).
 * SIP types are not mocked to avoid issues with Mockito and javax.sip on some JVMs.
 */
class ScaipSipListenerTest {

    private static final LogService LOG_SERVICE = new LogService();

    private ScaipSipListener listenerWithForwarder(JsonForwarder forwarder) {
        return new ScaipSipListener(LOG_SERVICE, forwarder);
    }

    /** No-op forwarder for tests that need valid parse → ACK without real HTTP. */
    private static final JsonForwarder NO_OP_ACK_FORWARDER = new JsonForwarder() {
        @Override
        public boolean forward(String json) {
            return true;
        }
    };

    private static final String MINIMAL_VALID_BODY =
        "<?xml version=\"1.0\"?><scaip><ref>r1</ref><cid>c</cid><dty>d</dty>"
            + "<sco></sco><cha></cha><mty></mty><hbo></hbo><did></did><dco></dco><dte></dte>"
            + "<crd></crd><stc></stc><stt></stt><pri></pri><lco></lco><lva></lva><lte></lte>"
            + "<ico></ico><ite></ite><ame></ame></scaip>";

    @Test
    void buildResponseBody_returnsAckForValidXmlWithApplicationXmlContentType() {
        String responseBody = listenerWithForwarder(NO_OP_ACK_FORWARDER).buildResponseBody(MINIMAL_VALID_BODY, "application/xml");

        assertNotNull(responseBody);
        assertTrue(responseBody.contains("<ref>r1</ref>"));
        assertTrue(responseBody.contains("<snu>0</snu>"));
        assertEquals("ACK", ScaipXml.parseResponseResult(responseBody));
    }

    @Test
    void buildResponseBody_returnsNackWhenContentTypeIsNotApplicationXml() {
        String responseBody = listenerWithForwarder(null).buildResponseBody(MINIMAL_VALID_BODY, "text/plain");

        assertNotNull(responseBody);
        assertTrue(responseBody.contains("<snu>2</snu>"));
        assertTrue(responseBody.contains("Content-Type must be application/xml"));
    }

    @Test
    void buildResponseBody_returnsNackWhenContentTypeIsNull() {
        String responseBody = listenerWithForwarder(null).buildResponseBody(MINIMAL_VALID_BODY, null);

        assertNotNull(responseBody);
        assertTrue(responseBody.contains("<snu>2</snu>"));
    }

    @Test
    void buildResponseBody_acceptsContentTypeWithWhitespace() {
        String responseBody = listenerWithForwarder(NO_OP_ACK_FORWARDER).buildResponseBody(MINIMAL_VALID_BODY, "  application/xml  ");

        assertTrue(responseBody.contains("<snu>0</snu>"));
    }

    @Test
    void buildResponseBody_returnsNackWhenRequiredTagMissing() {
        String body = "<?xml version=\"1.0\"?><scaip><ref>r1</ref><cid>c</cid></scaip>";
        String responseBody = listenerWithForwarder(null).buildResponseBody(body, "application/xml");

        assertTrue(responseBody.contains("<snu>7</snu>"));
        assertTrue(responseBody.contains("Missing required") || responseBody.contains("dty"));
    }

    @Test
    void buildResponseBody_echoesRefInNackWhenParseFails() {
        String body = "<?xml version=\"1.0\"?><scaip><ref>my-ref</ref><cid>c</cid></scaip>";
        String responseBody = listenerWithForwarder(null).buildResponseBody(body, "application/xml");

        assertTrue(responseBody.contains("<ref>my-ref</ref>"));
    }

    @Test
    void buildResponseBody_returnsNackSnu2ForInvalidXmlMissingClosingTag() {
        String body = "<?xml version=\"1.0\"?><scaip><ref>r1</ref><cid>c</cid><dty>d</dty>";
        // Missing closing </scaip> causes parse failure -> snu=2 (Invalid format) per SCAIP spec
        String responseBody = listenerWithForwarder(null).buildResponseBody(body, "application/xml");

        assertNotNull(responseBody);
        assertEquals("NACK", ScaipXml.parseResponseResult(responseBody));
        assertTrue(responseBody.contains("<snu>2</snu>"));
        String ste = ScaipXml.parseResponseStatusText(responseBody);
        assertNotNull(ste);
        assertTrue(ste.contains("Invalid XML"), "status_text should indicate invalid XML: " + ste);
    }

    @Test
    void buildResponseBody_returnsNackSnu2ForEmptyBody() {
        String responseBody = listenerWithForwarder(null).buildResponseBody("", "application/xml");
        assertNotNull(responseBody);
        assertTrue(responseBody.contains("<snu>2</snu>"));
        assertTrue(ScaipXml.parseResponseStatusText(responseBody).contains("Empty"));
    }

    @Test
    void buildResponseBody_returnsNackWhenForwarderFails() {
        JsonForwarder failingForwarder = new JsonForwarder() {
            @Override
            public boolean forward(String json) {
                return false;
            }
        };
        String responseBody = listenerWithForwarder(failingForwarder).buildResponseBody(MINIMAL_VALID_BODY, "application/xml");

        assertNotNull(responseBody);
        assertEquals("NACK", ScaipXml.parseResponseResult(responseBody));
        assertTrue(responseBody.contains("<snu>5</snu>"));
        assertTrue(responseBody.contains("Forward failed"));
    }

    @Test
    void buildResponseBody_returnsNackWhenForwarderIsNull() {
        String responseBody = listenerWithForwarder(null).buildResponseBody(MINIMAL_VALID_BODY, "application/xml");
        assertNotNull(responseBody);
        assertEquals("NACK", ScaipXml.parseResponseResult(responseBody));
        assertTrue(responseBody.contains("<snu>5</snu>"));
        assertTrue(responseBody.contains("Forwarder not configured"));
    }

    @Test
    void buildResponseBody_returnsNackWhenForwardReturnsFalse() {
        JsonForwarder failingForwarder = new JsonForwarder() {
            @Override
            public boolean forward(String json) {
                return false;
            }
        };
        String responseBody = listenerWithForwarder(failingForwarder).buildResponseBody(MINIMAL_VALID_BODY, "application/xml");

        assertNotNull(responseBody);
        assertEquals("NACK", ScaipXml.parseResponseResult(responseBody));
        assertEquals("5", ScaipXml.parseResponseStatusNumber(responseBody));
        assertTrue(ScaipXml.parseResponseStatusText(responseBody).contains("Forward failed"));
    }
}
