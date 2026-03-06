package com.syntilio.scaip.server;

import org.junit.jupiter.api.Test;
import com.syntilio.scaip.domain.ScaipXml;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class LogServiceTest {

    @Test
    void logIncoming_doesNotThrow() {
        LogService logService = new LogService();
        assertDoesNotThrow(() -> logService.logIncoming(""));
        assertDoesNotThrow(() -> logService.logIncoming("some message"));
    }

    @Test
    void logEvent_doesNotThrow() {
        LogService logService = new LogService();
        ScaipXml.ParseResult event = ScaipXml.ParseResult.success(
            "ref", "1", "cid", "dty", "sco", "cha", "mty", "hbo",
            "did", "dco", "dte", "crd", "stc", "stt", "pri",
            "lco", "lva", "lte", "ico", "ite", "ame");
        assertDoesNotThrow(() -> logService.logEvent(event));
    }
}
