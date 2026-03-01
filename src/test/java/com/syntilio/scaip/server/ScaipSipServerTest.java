package com.syntilio.scaip.server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ScaipSipServerTest {

    @Test
    void constructorStoresHostAndPort() {
        ScaipSipServer server = new ScaipSipServer("192.168.1.1", 5060);
        // No getters exposed; we only verify construction and stop() is safe before start()
        assertDoesNotThrow(() -> server.stop());
    }
}
