package com.syntilio.scaip.client;

import com.syntilio.scaip.domain.ScaipRequest;
import com.syntilio.scaip.domain.ScaipXml;
import com.syntilio.scaip.enums.DeviceComponent;
import com.syntilio.scaip.enums.DeviceType;
import com.syntilio.scaip.enums.StatusCode;

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
            ScaipRequest alarmReq = ScaipRequest.alarm(ref1, DeviceType.FIXED_TRIGGER, DeviceComponent.SWITCH_1, StatusCode.MANUAL_ALARM);
            System.out.println("Sending: SCAIP alarm (cid=+123456, dty=" + DeviceType.FIXED_TRIGGER.getCode() + ", did=001d940cb800, stc=" + StatusCode.MANUAL_ALARM.getCode() + ")");
            Response r1 = client.sendScaipXml(ScaipXml.buildRequest(alarmReq));
            printScaipResponse(r1);

            String ref2 = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            ScaipRequest heartbeatReq = ScaipRequest.heartbeat(ref2, DeviceType.FIXED_TRIGGER, StatusCode.NORMAL_STATE);
            System.out.println("Sending: SCAIP heartbeat (stc=" + StatusCode.NORMAL_STATE.getCode() + " normal)");
            Response r2 = client.sendScaipXml(ScaipXml.buildRequest(heartbeatReq));
            printScaipResponse(r2);

            ScaipRequest invalidReq = ScaipRequest.invalid(StatusCode.MANUAL_ALARM);
            System.out.println("Sending: Invalid SCAIP (missing dty) -> expect NACK");
            Response r3 = client.sendScaipXml(ScaipXml.buildRequest(invalidReq));
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
