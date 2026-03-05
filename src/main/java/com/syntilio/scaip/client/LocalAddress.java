package com.syntilio.scaip.client;

import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Discovers the local IP address that would be used to reach a given host
 * (e.g. the SIP trunk). Used for binding and, when not behind NAT, for Via and SDP.
 *
 * <p><b>Behind NAT:</b> This returns the private (LAN) IP. The trunk cannot reach that
 * address. SIP 200 OK often still works because many trunks send responses to the
 * request's source address (the NAT's public IP). For reliable NAT traversal, set
 * {@code VOIC_PUBLIC_HOST} to your public IP so Via and SDP advertise it (and forward
 * SIP and RTP ports on your router to this host).
 */
public final class LocalAddress {

    private LocalAddress() {}

    /**
     * Returns the local IPv4 address that would be used to send packets to the given host.
     * Creates a temporary UDP socket and "connects" to the target to determine the outgoing interface.
     *
     * @param targetHost host we intend to reach (e.g. trunk host)
     * @param targetPort port (e.g. SIP port)
     * @return local IP string, e.g. "192.168.1.10" or "127.0.0.1" (private IP when behind NAT)
     */
    public static String discoverLocalAddress(String targetHost, int targetPort) {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName(targetHost), targetPort);
            return socket.getLocalAddress().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }
}
