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

    private static final String MINIMAL_VALID_BODY =
        "<?xml version=\"1.0\"?><scaip><ref>r1</ref><cid>c</cid><dty>d</dty>"
            + "<sco></sco><cha></cha><mty></mty><hbo></hbo><did></did><dco></dco><dte></dte>"
            + "<crd></crd><stc></stc><stt></stt><pri></pri><lco></lco><lva></lva><lte></lte>"
            + "<ico></ico><ite></ite><ame></ame></scaip>";

    @Test
    void buildResponseBody_returnsAckForValidXmlWithApplicationXmlContentType() {
        String responseBody = ScaipSipListener.buildResponseBody(MINIMAL_VALID_BODY, "application/xml", LOG_SERVICE, null);

        assertNotNull(responseBody);
        assertTrue(responseBody.contains("<ref>r1</ref>"));
        assertTrue(responseBody.contains("<snu>0</snu>"));
        assertEquals("ACK", ScaipXml.parseResponseResult(responseBody));
    }

    @Test
    void buildResponseBody_returnsNackWhenContentTypeIsNotApplicationXml() {
        String responseBody = ScaipSipListener.buildResponseBody(MINIMAL_VALID_BODY, "text/plain", LOG_SERVICE, null);

        assertNotNull(responseBody);
        assertTrue(responseBody.contains("<snu>2</snu>"));
        assertTrue(responseBody.contains("Content-Type must be application/xml"));
    }

    @Test
    void buildResponseBody_returnsNackWhenContentTypeIsNull() {
        String responseBody = ScaipSipListener.buildResponseBody(MINIMAL_VALID_BODY, null, LOG_SERVICE, null);

        assertNotNull(responseBody);
        assertTrue(responseBody.contains("<snu>2</snu>"));
    }

    @Test
    void buildResponseBody_acceptsContentTypeWithWhitespace() {
        String responseBody = ScaipSipListener.buildResponseBody(MINIMAL_VALID_BODY, "  application/xml  ", LOG_SERVICE, null);

        assertTrue(responseBody.contains("<snu>0</snu>"));
    }

    @Test
    void buildResponseBody_returnsNackWhenRequiredTagMissing() {
        String body = "<?xml version=\"1.0\"?><scaip><ref>r1</ref><cid>c</cid></scaip>";
        String responseBody = ScaipSipListener.buildResponseBody(body, "application/xml", LOG_SERVICE, null);

        assertTrue(responseBody.contains("<snu>7</snu>"));
        assertTrue(responseBody.contains("Missing required") || responseBody.contains("dty"));
    }

    @Test
    void buildResponseBody_echoesRefInNackWhenParseFails() {
        String body = "<?xml version=\"1.0\"?><scaip><ref>my-ref</ref><cid>c</cid></scaip>";
        String responseBody = ScaipSipListener.buildResponseBody(body, "application/xml", LOG_SERVICE, null);

        assertTrue(responseBody.contains("<ref>my-ref</ref>"));
    }

    @Test
    void buildResponseBody_returnsNackForInvalidXmlMissingClosingTag() {
        String body = "<?xml version=\"1.0\"?><scaip><ref>r1</ref><cid>c</cid><dty>d</dty>";
        // Missing closing </scaip> causes parse failure
        String responseBody = ScaipSipListener.buildResponseBody(body, "application/xml", LOG_SERVICE, null);

        assertNotNull(responseBody);
        assertEquals("NACK", ScaipXml.parseResponseResult(responseBody));
        assertTrue(responseBody.contains("<snu>7</snu>"));
        String ste = ScaipXml.parseResponseStatusText(responseBody);
        assertNotNull(ste);
        assertTrue(ste.contains("Invalid XML"), "status_text should indicate invalid XML: " + ste);
    }

    @Test
    void buildResponseBody_returnsNackWhenForwarderFails() {
        JsonForwarder failingForwarder = new JsonForwarder() {
            @Override
            public boolean forward(String json) {
                return false;
            }
        };
        String responseBody = ScaipSipListener.buildResponseBody(MINIMAL_VALID_BODY, "application/xml", LOG_SERVICE, failingForwarder);

        assertNotNull(responseBody);
        assertEquals("NACK", ScaipXml.parseResponseResult(responseBody));
        assertTrue(responseBody.contains("<snu>5</snu>"));
        assertTrue(responseBody.contains("Forward failed"));
    }

    @Test
    void buildResponseBody_returnsNackWhenSimulateNon200() {
        JsonForwarder simulateNon200Forwarder = new JsonForwarder("https://httpbin.org/post", 10, true);
        String responseBody = ScaipSipListener.buildResponseBody(MINIMAL_VALID_BODY, "application/xml", LOG_SERVICE, simulateNon200Forwarder);

        assertNotNull(responseBody);
        assertEquals("NACK", ScaipXml.parseResponseResult(responseBody));
        assertEquals("5", ScaipXml.parseResponseStatusNumber(responseBody));
        assertTrue(ScaipXml.parseResponseStatusText(responseBody).contains("Forward failed"));
    }
}
