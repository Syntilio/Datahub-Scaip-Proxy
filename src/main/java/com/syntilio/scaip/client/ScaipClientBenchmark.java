package com.syntilio.scaip.client;

import com.syntilio.scaip.enums.DeviceComponent;
import com.syntilio.scaip.enums.DeviceType;
import com.syntilio.scaip.enums.StatusCode;

import java.util.UUID;

/**
 * Runs the SCAIP client performance test: many iterations of the standard
 * message set (alarm, heartbeat, invalid). Prints progress and timing to console.
 * <p>
 * Env: SCAIP_SERVER_HOST, SCAIP_SERVER_PORT, SCAIP_CLIENT_HOST, SCAIP_RUNS (default 10000).
 * Client port is chosen per run to avoid port conflicts.
 */
public class ScaipClientBenchmark {

    private static final int DEFAULT_RUNS = 100;
    private static final int PROGRESS_EVERY = 5;
    private static final int BASE_CLIENT_PORT = 5063;

    public static void main(String[] args) throws Exception {
        String serverHost = System.getenv().getOrDefault("SCAIP_SERVER_HOST", "127.0.0.1");
        int serverPort = Integer.parseInt(
            System.getenv().getOrDefault("SCAIP_SERVER_PORT", "5062")
        );
        String clientHost = System.getenv().getOrDefault("SCAIP_CLIENT_HOST", "127.0.0.1");
        String transport = System.getenv().getOrDefault("SCAIP_TRANSPORT", "udp");
        int runs = Integer.parseInt(
            System.getenv().getOrDefault("SCAIP_RUNS", String.valueOf(DEFAULT_RUNS))
        );

        System.out.println("SCAIP benchmark: " + serverHost + ":" + serverPort + " (" + transport + ")");
        System.out.println("Runs: " + runs + " (one alarm + heartbeat + invalid per run)");
        System.out.println();

        String alarm = buildAlarm();
        String heartbeat = buildHeartbeat();
        String invalid = buildInvalid();

        long startNs = System.nanoTime();
        int errors = 0;

        for (int i = 1; i <= runs; i++) {
            int clientPort = BASE_CLIENT_PORT + (i % 10_000);
            ScaipClient client = new ScaipClient(serverHost, serverPort, clientHost, clientPort, transport);
            try {
                client.start();
                if (client.sendScaipXml(alarm) == null) errors++;
                if (client.sendScaipXml(heartbeat) == null) errors++;
                if (client.sendScaipXml(invalid) == null) errors++;
            } catch (Exception e) {
                errors += 3;
            } finally {
                client.stop();
            }
            if (i % PROGRESS_EVERY == 0) {
                System.out.print(".");
            }
        }

        long endNs = System.nanoTime();
        double totalSec = (endNs - startNs) / 1_000_000_000.0;
        double avgSec = totalSec / runs;

        System.out.println();
        System.out.println();
        System.out.println("--- Benchmark results ---");
        System.out.println("Runs:        " + runs);
        System.out.println("Total time:  " + String.format("%.2f", totalSec) + "s");
        System.out.println("Average:     " + String.format("%.4f", avgSec) + "s per run");
        if (errors > 0) {
            System.out.println("Timeouts/err: " + errors);
        }
        System.out.println("----------------------------");
    }

    private static String buildAlarm() {
        String ref = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        return """
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
            ref);
    }

    private static String buildHeartbeat() {
        String ref = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        return """
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
            ref);
    }

    private static String buildInvalid() {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <scaip>
              <cid>+123456</cid>
              <did>001d940cb800</did>
              <stc>%s</stc>
            </scaip>""".formatted(StatusCode.MANUAL_ALARM.getCode());
    }
}
