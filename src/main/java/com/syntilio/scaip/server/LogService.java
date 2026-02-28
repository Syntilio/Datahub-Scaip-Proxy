package com.syntilio.scaip.server;

import com.syntilio.scaip.ScaipXml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogService {

    private static final Logger logger =
            LoggerFactory.getLogger(LogService.class);

    public void logIncoming(String message) {
        logger.info("=================================");
        logger.info("SCAIP MESSAGE RECEIVED");
        logger.info("{}", message);
        logger.info("=================================");
    }

    public void logEvent(ScaipXml.ParseResult event) {
        logger.info("SCAIP event: ref={} cid={} did={} dty={} stc={} lco={} lte={} pri={}",
            event.getRef(), event.getControllerId(), event.getDeviceId(), event.getDeviceType(), event.getStatusCode(),
            event.getLocationCode(), event.getLocationText(), event.getPriority());
    }
}
