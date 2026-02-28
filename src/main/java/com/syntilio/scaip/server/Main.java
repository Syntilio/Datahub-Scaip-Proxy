package com.syntilio.scaip.server;

public class Main {
    public static void main(String[] args) throws Exception {
        String host = System.getenv().getOrDefault("SCAIP_HOST", "127.0.0.1");
        int port = Integer.parseInt(
            System.getenv().getOrDefault("SCAIP_PORT", "5062")
        );

        ScaipSipServer server = new ScaipSipServer(host, port);
        server.start();

        System.out.println("SCAIP SIP backend started on " + host + ":" + port + " (UDP/TCP)");
    }
}
