package com.syntilio.scaip.client;

import com.syntilio.scaip.domain.ScaipXml;

import javax.sip.message.Response;
import java.util.UUID;

/**
 * Runs the SCAIP client: connects to the server (UDP or TCP) and sends
 * spec-format SCAIP XML messages from hardcoded samples (no POJO);
 * prints response ref, snu, ste from body.
 */
public class ScaipClientMain {

    public static void main(String[] args) throws Exception {
        String serverHost = System.getenv().getOrDefault("SCAIP_SERVER_HOST", "127.0.0.1");
        int serverPort = Integer.parseInt(
            System.getenv().getOrDefault("SCAIP_SERVER_PORT", "5060")
        );
        String clientHost = System.getenv().getOrDefault("SCAIP_CLIENT_HOST", "127.0.0.1");
        int clientPort = Integer.parseInt(
            System.getenv().getOrDefault("SCAIP_CLIENT_PORT", "5063")
        );
        String transport = System.getenv().getOrDefault("SCAIP_TRANSPORT", "udp");

        System.out.println("SCAIP client connecting to " + serverHost + ":" + serverPort + " (" + transport + ")");
        System.out.println("Client listening on " + clientHost + ":" + clientPort);
        System.out.println();

        ScaipClient client = new ScaipClient(serverHost, serverPort, clientHost, clientPort, transport);
        client.start();

        try {
            String ref1 = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            System.out.println("Sending: SCAIP alarm (cid=+123456, dty=0004, did=001d940cb800, stc=0010)");
            Response r1 = client.sendScaipXml(ScaipSampleXml.alarmWithRef(ref1));
            printScaipResponse(r1);

            String ref2 = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            System.out.println("Sending: SCAIP heartbeat (stc=0070 normal)");
            Response r2 = client.sendScaipXml(ScaipSampleXml.heartbeatWithRef(ref2));
            printScaipResponse(r2);

            System.out.println("Sending: Invalid SCAIP (missing dty) -> expect NACK");
            Response r3 = client.sendScaipXml(ScaipSampleXml.INVALID_MISSING_DTY);
            printScaipResponse(r3);

            System.out.println("Sending: Invalid SCAIP (malformed XML, missing closing tag, valid did) -> expect NACK");
            Response r4 = client.sendScaipXml(ScaipSampleXml.INVALID_MALFORMED_XML);
            printScaipResponse(r4);

            System.out.println("Done. All messages sent.");
        } finally {
            client.stop();
        }
        System.exit(0);
    }

    private static void printScaipResponse(Response response) {
        if (response == null) {
            System.out.println("  -> (no response or timeout)");
            return;
        }
        String body = ScaipClient.getResponseBody(response);
        String result = body != null ? ScaipXml.parseResponseResult(body) : null;
        String ref = body != null ? ScaipXml.parseResponseRef(body) : null;
        String snu = body != null ? ScaipXml.parseResponseStatusNumber(body) : null;
        String ste = body != null ? ScaipXml.parseResponseStatusText(body) : null;
        if ("ACK".equals(result)) {
            System.out.println("  -> 200 OK  ref=" + ref + " snu=" + snu + " (ACK)");
        } else if ("NACK".equals(result)) {
            System.out.println("  -> 200 OK  ref=" + ref + " snu=" + snu + " ste=" + (ste != null ? ste : "(none)"));
        } else {
            System.out.println("  -> " + response.getStatusCode() + " " + response.getReasonPhrase()
                + (body != null && !body.isEmpty() ? "  body=" + body.replace("\n", " ").trim() : ""));
        }
    }
}
