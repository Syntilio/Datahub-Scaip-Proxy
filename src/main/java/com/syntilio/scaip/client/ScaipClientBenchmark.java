package com.syntilio.scaip.client;

import com.syntilio.scaip.domain.ScaipRequest;
import com.syntilio.scaip.domain.ScaipXml;
import com.syntilio.scaip.enums.DeviceComponent;
import com.syntilio.scaip.enums.DeviceType;
import com.syntilio.scaip.enums.StatusCode;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Runs the SCAIP client performance test: many iterations of the standard
 * message set (alarm, heartbeat, invalid). Uses 3 client threads in parallel.
 * <p>
 * Env: SCAIP_SERVER_HOST, SCAIP_SERVER_PORT, SCAIP_CLIENT_HOST, SCAIP_RUNS (default 100),
 * SCAIP_THREADS (default 3).
 */
public class ScaipClientBenchmark {

    private static final int DEFAULT_RUNS = 50;
    private static final int DEFAULT_THREADS = 1;
    private static final int PROGRESS_EVERY = 5;
    private static final int BASE_CLIENT_PORT = 5063;

    public static void main(String[] args) throws Exception {
        String serverHost = System.getenv().getOrDefault("SCAIP_SERVER_HOST", "127.0.0.1");
        int serverPort = Integer.parseInt(
            System.getenv().getOrDefault("SCAIP_SERVER_PORT", "5060")
        );
        String clientHost = System.getenv().getOrDefault("SCAIP_CLIENT_HOST", "127.0.0.1");
        String transport = System.getenv().getOrDefault("SCAIP_TRANSPORT", "udp");
        int runs = Integer.parseInt(
            System.getenv().getOrDefault("SCAIP_RUNS", String.valueOf(DEFAULT_RUNS))
        );
        int numThreads = Integer.parseInt(
            System.getenv().getOrDefault("SCAIP_THREADS", String.valueOf(DEFAULT_THREADS))
        );

        System.out.println("SCAIP benchmark: " + serverHost + ":" + serverPort + " (" + transport + ")");
        System.out.println("Runs: " + runs + " with " + numThreads + " clients in parallel (alarm + heartbeat + invalid per run)");
        System.out.println();

        String alarm = ScaipXml.buildRequest(ScaipRequest.alarm(
            randomRef(), DeviceType.FIXED_TRIGGER, DeviceComponent.SWITCH_1, StatusCode.MANUAL_ALARM));
        String heartbeat = ScaipXml.buildRequest(ScaipRequest.heartbeat(
            randomRef(), DeviceType.FIXED_TRIGGER, StatusCode.NORMAL_STATE));
        String invalid = ScaipXml.buildRequest(ScaipRequest.invalid(StatusCode.MANUAL_ALARM));

        AtomicInteger errors = new AtomicInteger(0);
        AtomicInteger completed = new AtomicInteger(0);
        Object progressLock = new Object();

        long startNs = System.nanoTime();

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        List<Runnable> tasks = new ArrayList<>();
        for (int t = 0; t < numThreads; t++) {
            final int threadIndex = t;
            tasks.add(() -> {
                int myRuns = (runs + numThreads - 1 - threadIndex) / numThreads;
                int portOffset = threadIndex * 10_000;
                for (int i = 0; i < myRuns; i++) {
                    int clientPort = BASE_CLIENT_PORT + portOffset + (i % 10_000);
                    ScaipClient client = new ScaipClient(serverHost, serverPort, clientHost, clientPort, transport);
                    try {
                        client.start();
                        if (client.sendScaipXml(alarm) == null) errors.addAndGet(1);
                        if (client.sendScaipXml(heartbeat) == null) errors.addAndGet(1);
                        if (client.sendScaipXml(invalid) == null) errors.addAndGet(1);
                    } catch (Exception e) {
                        errors.addAndGet(3);
                    } finally {
                        // Let stack finish processing responses before stopping (avoids "SIP Stack Timer has been stopped")
                        try { Thread.sleep(150); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
                        client.stop();
                    }
                    int done = completed.incrementAndGet();
                    if (done % PROGRESS_EVERY == 0) {
                        synchronized (progressLock) {
                            System.out.print(".");
                        }
                    }
                }
            });
        }
        for (Runnable task : tasks) {
            executor.submit(task);
        }
        executor.shutdown();
        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

        long endNs = System.nanoTime();
        double totalSec = (endNs - startNs) / 1_000_000_000.0;
        double avgSec = totalSec / runs;

        System.out.println();
        System.out.println();
        System.out.println("--- Benchmark results ---");
        System.out.println("Runs:        " + runs);
        System.out.println("Threads:     " + numThreads);
        System.out.println("Total time:  " + String.format("%.2f", totalSec) + "s");
        System.out.println("Average:     " + String.format("%.4f", avgSec) + "s per run");
        if (errors.get() > 0) {
            System.out.println("Timeouts/err: " + errors.get());
        }
        System.out.println("----------------------------");
        System.exit(0);
    }

    private static String randomRef() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
