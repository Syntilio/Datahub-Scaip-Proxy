package com.syntilio.scaip;

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
        logger.info("SCAIP event: controllerId={} deviceId={} deviceType={} statusCode={} location={} priority={}",
            event.getControllerId(), event.getDeviceId(), event.getDeviceType(), event.getStatusCode(),
            event.getLocation(), event.getPriority());
    }
}