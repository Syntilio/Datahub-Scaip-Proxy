package com.syntilio.scaip.client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

/**
 * Sends a short peep tone (sine wave) as RTP PCMU (payload type 0) to a remote address.
 * Uses 8 kHz, 20 ms frames (160 samples = 160 bytes per packet).
 */
public class RtpPeepSender {

    private static final int SAMPLE_RATE = 8000;
    private static final int FRAME_MS = 20;
    private static final int SAMPLES_PER_FRAME = SAMPLE_RATE * FRAME_MS / 1000; // 160
    private static final int RTP_HEADER_LEN = 12;
    private static final int PCMU_PT = 0;

    private final String remoteHost;
    private final int remotePort;
    private final int peepHz;
    private final int peepMs;
    private final int localRtpPort;

    public RtpPeepSender(String remoteHost, int remotePort, int localRtpPort, int peepHz, int peepMs) {
        this.remoteHost = remoteHost;
        this.remotePort = remotePort;
        this.localRtpPort = localRtpPort;
        this.peepHz = peepHz;
        this.peepMs = peepMs;
    }

    /**
     * Generates a sine wave and sends it as RTP PCMU packets.
     *
     * @param ssrc SSRC for RTP (e.g. from dialog or random)
     */
    public void sendPeep(long ssrc) throws SocketException, IOException {
        int numSamples = SAMPLE_RATE * peepMs / 1000;
        short[] pcm = new short[numSamples];
        double amplitude = 8000.0;
        for (int i = 0; i < numSamples; i++) {
            double t = (double) i / SAMPLE_RATE;
            pcm[i] = (short) Math.round(amplitude * Math.sin(2 * Math.PI * peepHz * t));
        }
        byte[] pcmu = PcmuEncoder.encode(pcm);

        try (DatagramSocket socket = new DatagramSocket(localRtpPort)) {
            InetAddress remote = InetAddress.getByName(remoteHost);
            int seq = (int) (System.currentTimeMillis() % 65536);
            int timestamp = 0;
            long startMs = System.currentTimeMillis();

            for (int offset = 0, frameIndex = 0; offset < pcmu.length; offset += SAMPLES_PER_FRAME, frameIndex++) {
                int len = Math.min(SAMPLES_PER_FRAME, pcmu.length - offset);
                if (len <= 0) break;

                long targetMs = startMs + frameIndex * FRAME_MS;
                long nowMs = System.currentTimeMillis();
                if (nowMs < targetMs) {
                    Thread.sleep(targetMs - nowMs);
                }

                byte[] rtp = new byte[RTP_HEADER_LEN + len];
                rtp[0] = (byte) 0x80;
                rtp[1] = (byte) (PCMU_PT & 0x7F);
                rtp[2] = (byte) (seq >> 8);
                rtp[3] = (byte) seq;
                rtp[4] = (byte) (timestamp >> 24);
                rtp[5] = (byte) (timestamp >> 16);
                rtp[6] = (byte) (timestamp >> 8);
                rtp[7] = (byte) timestamp;
                rtp[8] = (byte) (ssrc >> 24);
                rtp[9] = (byte) (ssrc >> 16);
                rtp[10] = (byte) (ssrc >> 8);
                rtp[11] = (byte) ssrc;
                System.arraycopy(pcmu, offset, rtp, RTP_HEADER_LEN, len);

                DatagramPacket packet = new DatagramPacket(rtp, rtp.length, remote, remotePort);
                socket.send(packet);

                seq++;
                timestamp += SAMPLES_PER_FRAME;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while sending RTP", e);
        }
    }
}
