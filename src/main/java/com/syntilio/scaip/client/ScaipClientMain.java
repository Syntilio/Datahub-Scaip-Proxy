package com.syntilio.scaip.client;

import com.syntilio.scaip.domain.DeviceComponent;
import com.syntilio.scaip.domain.ScaipXml;
import com.syntilio.scaip.domain.DeviceType;
import com.syntilio.scaip.domain.StatusCode;

import javax.sip.message.Response;
import java.util.UUID;

/**
 * Runs the SCAIP client: connects to the server (UDP or TCP) and sends
 * spec-format SCAIP XML messages (short tags: ver, cid, dty, did, stc, ref, etc.);
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
            // 1. Valid SCAIP alarm (spec: cid, dty required; did, stc, ref optional but typical)
            String alarm = """
                <?xml version="1.0" encoding="UTF-8"?>
                <scaip>
                  <ver>01.00</ver>
                  <cid>+123456</cid>
                  <dty>%s</dty>
                  <did>001d940cb800</did>
                  <dco>%s</dco>
                  <stc>%s</stc>
                  <lco>021</lco>
                  <lte>kitchen</lte>
                  <pri>0</pri>
                  <ref>%s</ref>
                </scaip>""".formatted(
                DeviceType.FIXED_TRIGGER.getCode(),
                DeviceComponent.SWITCH_1.getCode(),
                StatusCode.MANUAL_ALARM.getCode(),
                ref1);
            System.out.println("Sending: SCAIP alarm (cid=+123456, dty=" + DeviceType.FIXED_TRIGGER.getCode() + ", did=001d940cb800, stc=" + StatusCode.MANUAL_ALARM.getCode() + ")");
            Response r1 = client.sendScaipXml(alarm);
            printScaipResponse(r1);

            String ref2 = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            // 2. Valid SCAIP heartbeat / normal (mty=PI optional for heartbeat)
            String heartbeat = """
                <?xml version="1.0" encoding="UTF-8"?>
                <scaip>
                  <ver>01.00</ver>
                  <cid>+123456</cid>
                  <dty>%s</dty>
                  <did>001d940cb800</did>
                  <stc>%s</stc>
                  <ref>%s</ref>
                </scaip>""".formatted(
                DeviceType.FIXED_TRIGGER.getCode(),
                StatusCode.NORMAL_STATE.getCode(),
                ref2);
            System.out.println("Sending: SCAIP heartbeat (stc=" + StatusCode.NORMAL_STATE.getCode() + " normal)");
            Response r2 = client.sendScaipXml(heartbeat);
            printScaipResponse(r2);

            // 3. Invalid SCAIP (missing required dty) -> expect NACK snu=7
            String invalid = """
                <?xml version="1.0" encoding="UTF-8"?>
                <scaip>
                  <cid>+123456</cid>
                  <did>001d940cb800</did>
                  <stc>%s</stc>
                </scaip>""".formatted(StatusCode.MANUAL_ALARM.getCode());
            System.out.println("Sending: Invalid SCAIP (missing dty) -> expect NACK");
            Response r3 = client.sendScaipXml(invalid);
            printScaipResponse(r3);

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
