package com.syntilio.scaip.server;

import javax.sip.*;
import javax.sip.header.HeaderFactory;
import javax.sip.message.MessageFactory;
import java.util.Properties;

public class ScaipSipServer {

    private final String host;
    private final int port;
    private SipStack sipStack;
    private SipProvider sipProvider;

    public ScaipSipServer(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void start() throws Exception {
        SipFactory sipFactory = SipFactory.getInstance();
        sipFactory.setPathName("gov.nist");

        Properties props = new Properties();
        props.setProperty("javax.sip.STACK_NAME", "scaip");
        props.setProperty("javax.sip.IP_ADDRESS", host);
        int workers = Integer.parseInt(
            System.getenv().getOrDefault("SCAIP_WORKERS", "10")
        );
        props.setProperty("gov.nist.javax.sip.THREAD_POOL_SIZE", String.valueOf(workers));

        sipStack = sipFactory.createSipStack(props);
        sipStack.start();

        MessageFactory messageFactory = sipFactory.createMessageFactory();
        HeaderFactory headerFactory = sipFactory.createHeaderFactory();
        ScaipSipListener listener = new ScaipSipListener();
        listener.setMessageFactory(messageFactory);
        listener.setHeaderFactory(headerFactory);

        sipProvider = sipStack.createSipProvider(sipStack.createListeningPoint(host, port, "UDP"));
        sipProvider.addSipListener(listener);

        SipProvider tcpProvider = sipStack.createSipProvider(sipStack.createListeningPoint(host, port, "TCP"));
        tcpProvider.addSipListener(listener);
    }

    public void stop() {
        if (sipStack != null) {
            sipStack.stop();
        }
    }
}
