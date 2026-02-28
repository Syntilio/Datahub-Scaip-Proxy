package com.syntilio.scaip.client;

import com.syntilio.scaip.ScaipXml;

import javax.sip.*;
import javax.sip.address.*;
import javax.sip.header.*;
import javax.sip.message.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

/**
 * SCAIP client that connects to the SCAIP SIP backend (no TLS) and sends
 * SIP MESSAGE requests with SCAIP message bodies.
 */
public class ScaipClient implements SipListener {

    private final String serverHost;
    private final int serverPort;
    private final String clientHost;
    private final int clientPort;
    private final String transport;

    private SipStack sipStack;
    private SipProvider sipProvider;
    private AddressFactory addressFactory;
    private HeaderFactory headerFactory;
    private MessageFactory messageFactory;

    private final BlockingQueue<Response> responseQueue = new LinkedBlockingQueue<>();

    public ScaipClient(String serverHost, int serverPort, String clientHost, int clientPort, String transport) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.clientHost = clientHost;
        this.clientPort = clientPort;
        this.transport = transport != null ? transport.toLowerCase(Locale.ROOT) : "udp";
    }

    public void start() throws Exception {
        SipFactory sipFactory = SipFactory.getInstance();
        sipFactory.setPathName("gov.nist");

        Properties props = new Properties();
        // Unique name per client so concurrent threads each have their own stack
        props.setProperty("javax.sip.STACK_NAME", "scaip-client-" + clientHost + "-" + clientPort);
        props.setProperty("javax.sip.IP_ADDRESS", clientHost);

        sipStack = sipFactory.createSipStack(props);
        sipStack.start();

        addressFactory = sipFactory.createAddressFactory();
        headerFactory = sipFactory.createHeaderFactory();
        messageFactory = sipFactory.createMessageFactory();

        ListeningPoint lp = sipStack.createListeningPoint(clientHost, clientPort, transport);
        sipProvider = sipStack.createSipProvider(lp);
        sipProvider.addSipListener(this);
    }

    public void stop() {
        if (sipStack != null) {
            sipStack.stop();
        }
    }

    /**
     * Sends a SCAIP MESSAGE to the server and waits for 200 OK (with timeout).
     *
     * @param body the SCAIP message body (e.g. text or JSON)
     * @param contentType optional Content-Type (e.g. "text/plain", "application/json"). If null, "text/plain" is used.
     * @return the server Response, or null if timeout
     */
    public Response sendMessage(String body, String contentType) throws Exception {
        SipURI requestUri = addressFactory.createSipURI("scaip", serverHost);
        requestUri.setPort(serverPort);

        String fromTag = "client-" + UUID.randomUUID().toString().substring(0, 8);
        Address fromAddress = addressFactory.createAddress("sip:client@" + clientHost + ":" + clientPort);
        FromHeader fromHeader = headerFactory.createFromHeader(fromAddress, fromTag);

        Address toAddress = addressFactory.createAddress("sip:scaip@" + serverHost + ":" + serverPort);
        ToHeader toHeader = headerFactory.createToHeader(toAddress, null);

        String callId = UUID.randomUUID().toString();
        CallIdHeader callIdHeader = headerFactory.createCallIdHeader(callId);

        long cSeq = System.currentTimeMillis() % 100000;
        CSeqHeader cSeqHeader = headerFactory.createCSeqHeader(cSeq, Request.MESSAGE);

        String branchId = "z9hG4bK-" + UUID.randomUUID().toString().replace("-", "");
        ViaHeader viaHeader = headerFactory.createViaHeader(clientHost, clientPort, transport, branchId);
        List<ViaHeader> viaHeaders = new ArrayList<>();
        viaHeaders.add(viaHeader);

        MaxForwardsHeader maxForwardsHeader = headerFactory.createMaxForwardsHeader(70);

        byte[] content = body != null ? body.getBytes(StandardCharsets.UTF_8) : new byte[0];
        String ct = contentType != null ? contentType : "text/plain";
        String[] parts = ct.split("/", 2);
        ContentTypeHeader contentTypeHeader = headerFactory.createContentTypeHeader(
            parts.length == 2 ? parts[0].trim() : "text",
            parts.length == 2 ? parts[1].trim() : "plain"
        );

        Request request = messageFactory.createRequest(
            requestUri,
            Request.MESSAGE,
            callIdHeader,
            cSeqHeader,
            fromHeader,
            toHeader,
            viaHeaders,
            maxForwardsHeader,
            contentTypeHeader,
            content
        );

        ClientTransaction clientTransaction = sipProvider.getNewClientTransaction(request);
        clientTransaction.sendRequest();

        Response response = responseQueue.poll(5, TimeUnit.SECONDS);
        return response;
    }

    /** Convenience: send plain-text SCAIP message. */
    public Response sendMessage(String body) throws Exception {
        return sendMessage(body, "text/plain");
    }

    /** Sends a SCAIP XML message (Content-Type: application/xml). */
    public Response sendScaipXml(String xml) throws Exception {
        return sendMessage(xml != null ? xml : "", ScaipXml.CONTENT_TYPE);
    }

    /** Returns the response body as string, or null. */
    public static String getResponseBody(Response response) {
        if (response == null) return null;
        Object content = response.getContent();
        if (content == null) return null;
        if (content instanceof byte[]) {
            return new String((byte[]) content, StandardCharsets.UTF_8);
        }
        return content.toString();
    }

    @Override
    public void processRequest(RequestEvent requestEvent) {
        // Client does not expect incoming requests (except possibly for future use)
    }

    @Override
    public void processResponse(ResponseEvent responseEvent) {
        Response response = responseEvent.getResponse();
        responseQueue.offer(response);
    }

    @Override
    public void processTimeout(TimeoutEvent timeoutEvent) {
        responseQueue.offer(null);
    }

    @Override
    public void processIOException(IOExceptionEvent exceptionEvent) {
        responseQueue.offer(null);
    }

    @Override
    public void processTransactionTerminated(TransactionTerminatedEvent transactionTerminatedEvent) {
        // No special handling
    }

    @Override
    public void processDialogTerminated(DialogTerminatedEvent dialogTerminatedEvent) {
        // No special handling
    }
}
