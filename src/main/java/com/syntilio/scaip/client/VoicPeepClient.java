package com.syntilio.scaip.client;

import javax.sip.*;
import javax.sip.address.*;
import javax.sip.header.*;
import javax.sip.message.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

/**
 * Test client that places a call to a voic SIP trunk (INVITE), waits for 200 OK,
 * sends ACK, delivers a short peep tone via RTP (PCMU), then sends BYE.
 */
public class VoicPeepClient implements SipListener {

    private final String trunkHost;
    private final int trunkPort;
    private final String trunkUser; // e.g. "voic" or destination number
    /** Local address we bind to (SIP stack, listening point). */
    private final String clientHost;
    /** Address we put in Via, From, and SDP (same as clientHost unless behind NAT and VOIC_PUBLIC_HOST set). */
    private final String advertisedHost;
    private final int clientPort;
    private final int localRtpPort;
    private final String transport;
    private final int peepHz;
    private final int peepMs;

    private SipStack sipStack;
    private SipProvider sipProvider;
    private AddressFactory addressFactory;
    private HeaderFactory headerFactory;
    private MessageFactory messageFactory;

    private final BlockingQueue<Response> inviteResponseQueue = new LinkedBlockingQueue<>();

    public VoicPeepClient(
        String trunkHost,
        int trunkPort,
        String trunkUser,
        String clientHost,
        int clientPort,
        int localRtpPort,
        String transport,
        int peepHz,
        int peepMs
    ) {
        this(trunkHost, trunkPort, trunkUser, clientHost, null, clientPort, localRtpPort, transport, peepHz, peepMs);
    }

    /**
     * @param advertisedHost when non-null/non-empty, used in Via, From, and SDP instead of clientHost (for NAT: set to public IP).
     */
    public VoicPeepClient(
        String trunkHost,
        int trunkPort,
        String trunkUser,
        String clientHost,
        String advertisedHost,
        int clientPort,
        int localRtpPort,
        String transport,
        int peepHz,
        int peepMs
    ) {
        this.trunkHost = trunkHost;
        this.trunkPort = trunkPort;
        this.trunkUser = trunkUser != null && !trunkUser.isEmpty() ? trunkUser : "voic";
        this.clientHost = clientHost;
        this.advertisedHost = (advertisedHost != null && !advertisedHost.trim().isEmpty()) ? advertisedHost.trim() : clientHost;
        this.clientPort = clientPort;
        this.localRtpPort = localRtpPort;
        this.transport = transport != null ? transport.toLowerCase(Locale.ROOT) : "udp";
        this.peepHz = peepHz > 0 ? peepHz : 1000;
        this.peepMs = peepMs > 0 ? peepMs : 200;
    }

    public void start() throws Exception {
        SipFactory sipFactory = SipFactory.getInstance();
        sipFactory.setPathName("gov.nist");

        Properties props = new Properties();
        props.setProperty("javax.sip.STACK_NAME", "voic-peep-" + clientHost + "-" + clientPort);
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
     * Places a call to the trunk, sends peep tone over RTP, then hangs up.
     *
     * @return true if call was answered (200 OK) and peep was sent, false on failure or non-2xx
     */
    public boolean callAndSendPeep() throws Exception {
        String fromTag = "peep-" + UUID.randomUUID().toString().substring(0, 8);
        SipURI requestUri = addressFactory.createSipURI(trunkUser, trunkHost);
        if (trunkPort > 0) requestUri.setPort(trunkPort);

        Address fromAddress = addressFactory.createAddress("sip:peep@" + advertisedHost + ":" + clientPort);
        FromHeader fromHeader = headerFactory.createFromHeader(fromAddress, fromTag);

        Address toAddress = addressFactory.createAddress(requestUri);
        ToHeader toHeader = headerFactory.createToHeader(toAddress, null);

        CallIdHeader callIdHeader = headerFactory.createCallIdHeader(UUID.randomUUID().toString());
        long cSeq = 1;
        CSeqHeader cSeqHeader = headerFactory.createCSeqHeader(cSeq, Request.INVITE);

        String branchId = "z9hG4bK-" + UUID.randomUUID().toString().replace("-", "");
        ViaHeader viaHeader = headerFactory.createViaHeader(advertisedHost, clientPort, transport, branchId);
        List<ViaHeader> viaHeaders = new ArrayList<>();
        viaHeaders.add(viaHeader);

        MaxForwardsHeader maxForwardsHeader = headerFactory.createMaxForwardsHeader(70);

        String sdpOffer = buildSdpOffer(advertisedHost, localRtpPort);
        byte[] sdpBytes = sdpOffer.getBytes(StandardCharsets.UTF_8);
        ContentTypeHeader contentTypeHeader = headerFactory.createContentTypeHeader("application", "sdp");

        Request invite = messageFactory.createRequest(
            requestUri,
            Request.INVITE,
            callIdHeader,
            cSeqHeader,
            fromHeader,
            toHeader,
            viaHeaders,
            maxForwardsHeader,
            contentTypeHeader,
            sdpBytes
        );

        ClientTransaction inviteTx = sipProvider.getNewClientTransaction(invite);
        inviteTx.sendRequest();

        Response response = inviteResponseQueue.poll(30, TimeUnit.SECONDS);
        if (response == null || response.getStatusCode() != 200) {
            return false;
        }

        Dialog dialog = inviteTx.getDialog();
        if (dialog == null) {
            return false;
        }

        CSeqHeader okCSeq = (CSeqHeader) response.getHeader(CSeqHeader.NAME);
        Request ackReq = dialog.createAck(okCSeq.getSeqNumber());
        ClientTransaction ackTx = sipProvider.getNewClientTransaction(ackReq);
        ackTx.sendRequest();

        String sdpBody = getResponseBodyString(response);
        SdpParser.RemoteMedia remote = SdpParser.parseRemoteMedia(sdpBody);
        if (remote == null) {
            return false;
        }

        long ssrc = Math.abs(UUID.randomUUID().getLeastSignificantBits() & 0xFFFFFFFFL);
        RtpPeepSender sender = new RtpPeepSender(
            remote.connectionAddress,
            remote.audioPort,
            localRtpPort,
            peepHz,
            peepMs
        );
        sender.sendPeep(ssrc);

        Request bye = dialog.createRequest(Request.BYE);
        ClientTransaction byeTx = sipProvider.getNewClientTransaction(bye);
        byeTx.sendRequest();

        return true;
    }

    private static String buildSdpOffer(String ourIp, int rtpPort) {
        return "v=0\r\n" +
            "o=- 1 1 IN IP4 " + ourIp + "\r\n" +
            "s=-\r\n" +
            "c=IN IP4 " + ourIp + "\r\n" +
            "t=0 0\r\n" +
            "m=audio " + rtpPort + " RTP/AVP 0\r\n" +
            "a=rtpmap:0 PCMU/8000\r\n";
    }

    private static String getResponseBodyString(Response response) {
        Object content = response.getContent();
        if (content == null) return null;
        if (content instanceof byte[]) {
            return new String((byte[]) content, StandardCharsets.UTF_8);
        }
        return content.toString();
    }

    @Override
    public void processRequest(RequestEvent requestEvent) {
        // UAC; we don't expect incoming requests except possibly re-INVITE
    }

    @Override
    public void processResponse(ResponseEvent responseEvent) {
        Response response = responseEvent.getResponse();
        ClientTransaction tx = responseEvent.getClientTransaction();
        if (tx != null && response.getStatusCode() == 200 && Request.INVITE.equals(tx.getRequest().getMethod())) {
            inviteResponseQueue.offer(response);
        }
    }

    @Override
    public void processTimeout(TimeoutEvent timeoutEvent) {
        inviteResponseQueue.offer(null);
    }

    @Override
    public void processIOException(IOExceptionEvent exceptionEvent) {
        inviteResponseQueue.offer(null);
    }

    @Override
    public void processTransactionTerminated(TransactionTerminatedEvent transactionTerminatedEvent) {
    }

    @Override
    public void processDialogTerminated(DialogTerminatedEvent dialogTerminatedEvent) {
    }
}
