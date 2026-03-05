package com.syntilio.scaip.client;

/**
 * G.711 μ-law (PCMU) encoder for 16-bit linear PCM at 8 kHz.
 * Used to encode peep tone for RTP delivery to SIP voice trunks.
 */
public final class PcmuEncoder {

    private static final int BIAS = 0x84;
    private static final int MAX = 32635;

    private PcmuEncoder() {}

    /**
     * Encodes 16-bit signed PCM (little-endian) to 8-bit μ-law (PCMU).
     * Input: 2 bytes per sample, 8000 Hz mono.
     *
     * @param pcm 16-bit PCM samples (e.g. from a sine wave)
     * @return PCMU bytes, one per input sample
     */
    public static byte[] encode(short[] pcm) {
        byte[] out = new byte[pcm.length];
        for (int i = 0; i < pcm.length; i++) {
            out[i] = encodeSample(pcm[i]);
        }
        return out;
    }

    /**
     * Encodes a single 16-bit PCM sample to 8-bit μ-law.
     */
    public static byte encodeSample(short sample) {
        int sign = (sample & 0x8000) >>> 8;
        int magnitude = sample < 0 ? -sample : sample;
        if (magnitude > MAX) magnitude = MAX;
        magnitude += BIAS;
        int exponent = 7;
        for (int e = 0x4000; (magnitude & e) == 0 && exponent > 0; e >>= 1) {
            exponent--;
        }
        int mantissa = (magnitude >> (exponent + 3)) & 0x0F;
        int codeword = ~(sign | (exponent << 4) | mantissa);
        return (byte) (codeword & 0xFF);
    }
}
