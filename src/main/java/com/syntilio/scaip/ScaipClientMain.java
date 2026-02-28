package com.syntilio.scaip;

import javax.sip.message.Response;

/**
 * Runs the SCAIP client: connects to the server (no TLS) and sends
 * valid SCAIP XML messages; prints ACK/NACK from response body.
 */
public class ScaipClientMain {

    public static void main(String[] args) throws Exception {
        String serverHost = System.getenv().getOrDefault("SCAIP_SERVER_HOST", "127.0.0.1");
        int serverPort = Integer.parseInt(
            System.getenv().getOrDefault("SCAIP_SERVER_PORT", "5062")
        );
        String clientHost = System.getenv().getOrDefault("SCAIP_CLIENT_HOST", "127.0.0.1");
        int clientPort = Integer.parseInt(
            System.getenv().getOrDefault("SCAIP_CLIENT_PORT", "5063")
        );
        String transport = System.getenv().getOrDefault("SCAIP_TRANSPORT", "udp");

        System.out.println("SCAIP client connecting to " + serverHost + ":" + serverPort + " (" + transport + ", no TLS)");
        System.out.println("Client listening on " + clientHost + ":" + clientPort);
        System.out.println();

        ScaipClient client = new ScaipClient(serverHost, serverPort, clientHost, clientPort, transport);
        client.start();

        try {
            // 1. Valid SCAIP alarm event (all required fields)
            String alarm = """
                <?xml version="1.0" encoding="UTF-8"?>
                <scaip>
                  <controllerId>+123456</controllerId>
                  <deviceId>001d940cb800</deviceId>
                  <deviceType>fixedTrigger</deviceType>
                  <deviceComponent>unspecified</deviceComponent>
                  <statusCode>alarm</statusCode>
                  <location>kitchen</location>
                  <priority>5</priority>
                </scaip>""";
            System.out.println("Sending: SCAIP alarm event (controllerId=+123456, deviceId=001d940cb800)");
            Response r1 = client.sendScaipXml(alarm);
            printScaipResponse(r1);

            // 2. Valid SCAIP heartbeat / normal status
            String heartbeat = """
                <?xml version="1.0" encoding="UTF-8"?>
                <scaip>
                  <controllerId>+123456</controllerId>
                  <deviceId>001d940cb800</deviceId>
                  <deviceType>fixedTrigger</deviceType>
                  <statusCode>normal</statusCode>
                </scaip>""";
            System.out.println("Sending: SCAIP heartbeat (normal)");
            Response r2 = client.sendScaipXml(heartbeat);
            printScaipResponse(r2);

            // 3. Invalid SCAIP (missing required deviceId) -> expect NACK
            String invalid = """
                <?xml version="1.0" encoding="UTF-8"?>
                <scaip>
                  <controllerId>+123456</controllerId>
                  <deviceType>panicButton</deviceType>
                  <statusCode>alarm</statusCode>
                </scaip>""";
            System.out.println("Sending: Invalid SCAIP (missing deviceId) -> expect NACK");
            Response r3 = client.sendScaipXml(invalid);
            printScaipResponse(r3);

            System.out.println("Done. All messages sent.");
        } finally {
            client.stop();
        }
    }

    private static void printScaipResponse(Response response) {
        if (response == null) {
            System.out.println("  -> (no response or timeout)");
            return;
        }
        String body = ScaipClient.getResponseBody(response);
        String result = body != null ? ScaipXml.parseResponseResult(body) : null;
        if ("ACK".equals(result)) {
            System.out.println("  -> 200 OK  result=ACK");
        } else if ("NACK".equals(result)) {
            String reason = ScaipXml.parseResponseReason(body);
            System.out.println("  -> 200 OK  result=NACK  reason=" + (reason != null ? reason : "(none)"));
        } else {
            System.out.println("  -> " + response.getStatusCode() + " " + response.getReasonPhrase()
                + (body != null && !body.isEmpty() ? "  body=" + body.replace("\n", " ").trim() : ""));
        }
    }
}
