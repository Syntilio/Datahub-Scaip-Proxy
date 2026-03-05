package com.syntilio.scaip.client;

/**
 * Test client that calls a voic SIP trunk and sends a short peep tone (RTP PCMU).
 *
 * <p>Configure via environment (or defaults):
 * <ul>
 *   <li>VOIC_TRUNK_HOST – trunk host (default 127.0.0.1)</li>
 *   <li>VOIC_TRUNK_PORT – trunk SIP port (default 5060)</li>
 *   <li>VOIC_TRUNK_USER – user/destination in SIP URI (default voic)</li>
 *   <li>VOIC_CLIENT_HOST – local IP to bind for SIP (default: auto-detect from route to trunk)</li>
 *   <li>VOIC_PUBLIC_HOST – public IP for Via/SDP when behind NAT; forward SIP and RTP ports on router to this host</li>
 *   <li>VOIC_CLIENT_PORT – local SIP port (default 5064)</li>
 *   <li>VOIC_RTP_PORT – local RTP port for sending peep (default 10000)</li>
 *   <li>VOIC_PEEP_HZ – peep frequency in Hz (default 1000)</li>
 *   <li>VOIC_PEEP_MS – peep duration in ms (default 200)</li>
 *   <li>VOIC_TRANSPORT – udp or tcp (default udp)</li>
 * </ul>
 */
public class VoicPeepClientMain {

    public static void main(String[] args) throws Exception {
        String trunkHost = System.getenv().getOrDefault("VOIC_TRUNK_HOST", "127.0.0.1");
        int trunkPort = Integer.parseInt(System.getenv().getOrDefault("VOIC_TRUNK_PORT", "5060"));
        String trunkUser = System.getenv().getOrDefault("VOIC_TRUNK_USER", "voic");

        String clientHost = System.getenv().getOrDefault("VOIC_CLIENT_HOST", "").trim();
        if (clientHost.isEmpty()) {
            clientHost = LocalAddress.discoverLocalAddress(trunkHost, trunkPort);
        }
        String publicHost = System.getenv().getOrDefault("VOIC_PUBLIC_HOST", "").trim();
        int clientPort = Integer.parseInt(System.getenv().getOrDefault("VOIC_CLIENT_PORT", "5064"));
        int rtpPort = Integer.parseInt(System.getenv().getOrDefault("VOIC_RTP_PORT", "10000"));

        int peepHz = Integer.parseInt(System.getenv().getOrDefault("VOIC_PEEP_HZ", "1000"));
        int peepMs = Integer.parseInt(System.getenv().getOrDefault("VOIC_PEEP_MS", "200"));

        String transport = System.getenv().getOrDefault("VOIC_TRANSPORT", "udp");

        String advertised = publicHost.isEmpty() ? clientHost : publicHost;
        System.out.println("Voic peep client: calling sip:" + trunkUser + "@" + trunkHost + ":" + trunkPort + " (" + transport + ")");
        System.out.println("Local SIP " + clientHost + ":" + clientPort + (publicHost.isEmpty() ? "" : " (advertised " + advertised + ")") + ", RTP port " + rtpPort);
        System.out.println("Peep: " + peepHz + " Hz, " + peepMs + " ms");
        System.out.println();

        VoicPeepClient client = new VoicPeepClient(
            trunkHost, trunkPort, trunkUser,
            clientHost, publicHost.isEmpty() ? null : publicHost,
            clientPort, rtpPort,
            transport, peepHz, peepMs
        );
        client.start();

        try {
            boolean ok = client.callAndSendPeep();
            if (ok) {
                System.out.println("Call answered, peep sent, BYE sent.");
            } else {
                System.out.println("Call failed or no 200 OK (timeout / busy / reject).");
                System.exit(1);
            }
        } finally {
            client.stop();
        }
        System.exit(0);
    }
}
