package com.syntilio.scaip.client;

/**
 * Minimal SDP parser to extract remote connection address and audio media port from 200 OK.
 */
public final class SdpParser {

    private SdpParser() {}

    /**
     * Result of parsing SDP for remote RTP endpoint.
     */
    public static class RemoteMedia {
        public final String connectionAddress;
        public final int audioPort;

        public RemoteMedia(String connectionAddress, int audioPort) {
            this.connectionAddress = connectionAddress;
            this.audioPort = audioPort;
        }
    }

    /**
     * Parses SDP body (e.g. from 200 OK) and returns the first c= address and m=audio port.
     *
     * @param sdpBody SDP string (e.g. "v=0\r\n o=...\r\n c=IN IP4 192.168.1.1\r\n m=audio 40000 RTP/AVP 0\r\n")
     * @return remote media, or null if not found
     */
    public static RemoteMedia parseRemoteMedia(String sdpBody) {
        if (sdpBody == null || sdpBody.isEmpty()) return null;
        String connectionAddress = null;
        int audioPort = -1;
        for (String line : sdpBody.split("[\r\n]+")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("c=IN IP4 ")) {
                connectionAddress = trimmed.substring(9).trim();
            } else if (trimmed.startsWith("m=audio ")) {
                String rest = trimmed.substring(8).trim();
                int space = rest.indexOf(' ');
                if (space > 0) {
                    try {
                        audioPort = Integer.parseInt(rest.substring(0, space));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        if (connectionAddress != null && audioPort >= 0) {
            return new RemoteMedia(connectionAddress, audioPort);
        }
        return null;
    }
}
