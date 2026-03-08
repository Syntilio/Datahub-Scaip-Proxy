package com.syntilio.scaip.server;

import com.syntilio.scaip.domain.ScaipXml;
import com.syntilio.scaip.enums.StatusNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sip.*;
import javax.sip.header.CallIdHeader;
import javax.sip.header.HeaderFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;
import javax.sip.message.MessageFactory;
import java.nio.charset.StandardCharsets;

public class ScaipSipListener implements SipListener {

    private static final Logger log = LoggerFactory.getLogger(ScaipSipListener.class);

    private final LogService logService;
    private final JsonForwarder forwarder;
    private MessageFactory messageFactory;
    private HeaderFactory headerFactory;

    public ScaipSipListener() {
        this(new LogService(), new JsonForwarder());
    }

    /** Package-visible for unit tests that inject LogService and JsonForwarder. */
    ScaipSipListener(LogService logService, JsonForwarder forwarder) {
        this.logService = logService;
        this.forwarder = forwarder;
    }

    @Override
    public void processRequest(RequestEvent requestEvent) {
        Request request = requestEvent.getRequest();
        if (!Request.MESSAGE.equals(request.getMethod())) {
            log.info("Request ignored (method={}, expected MESSAGE)", request.getMethod());
            return;
        }
        String body = getBody(request);
        logService.logIncoming(body != null ? body : "");
        String contentType = getRequestContentType(request);
        ParsedRequest parsed = parseAndValidate(body, contentType);
        String responseBody;
        if (parsed.hasNack()) {
            responseBody = parsed.getNackBody();
        } else {
            logService.logEvent(parsed.getParseResult());
            boolean forwarded = forwarder == null || forwarder.forward(parsed.getJson());
            if (!forwarded) {
                log.warn("SCAIP message could not be forwarded (ref={}); sending NACK", parsed.getRef());
            }
            responseBody = forwarded
                ? ScaipXml.buildAck(parsed.getRef())
                : ScaipXml.buildNack(parsed.getRef(), StatusNumber.NOT_TREATED, "Forward failed");
        }
        try {
            ServerTransaction serverTransaction = getOrCreateServerTransaction(requestEvent, request);
            Response response = createOkResponseWithBody(request, responseBody);
            serverTransaction.sendResponse(response);
        } catch (Exception e) {
            logSendResponseFailure(request, responseBody, e);
            throw new RuntimeException("Failed to send SIP 200 OK", e);
        }
    }

    private ServerTransaction getOrCreateServerTransaction(RequestEvent requestEvent, Request request) throws SipException {
        ServerTransaction st = requestEvent.getServerTransaction();
        if (st == null) {
            SipProvider provider = (SipProvider) requestEvent.getSource();
            st = provider.getNewServerTransaction(request);
        }
        return st;
    }

    private Response createOkResponseWithBody(Request request, String responseBody) throws Exception {
        Response response = messageFactory.createResponse(Response.OK, request);
        response.setContent(
            responseBody.getBytes(StandardCharsets.UTF_8),
            headerFactory.createContentTypeHeader("application", "xml"));
        return response;
    }

    private void logSendResponseFailure(Request request, String responseBody, Exception e) {
        CallIdHeader callIdHeader = (CallIdHeader) request.getHeader(CallIdHeader.NAME);
        String callId = callIdHeader != null ? callIdHeader.getCallId() : null;
        log.error("Failed to send SIP 200 OK: method={} requestUri={} callId={} responseBodyLength={}",
            request.getMethod(), request.getRequestURI(), callId,
            responseBody != null ? responseBody.length() : 0, e);
    }

    /**
     * Parses and validates the request body and content-type.
     * Returns either a NACK body (to send as-is) or valid content (ref, json, parseResult) for logging and forwarding.
     */
    ParsedRequest parseAndValidate(String body, String contentType) {
        if (!ScaipXml.CONTENT_TYPE.equalsIgnoreCase(contentType != null ? contentType.trim() : "")) {
            return ParsedRequest.nack(ScaipXml.buildNack(null, StatusNumber.INVALID_FORMAT, "Content-Type must be application/xml"));
        }
        ScaipXml.ParseResult parsed = ScaipXml.parseRequest(body);
        String ref = parsed.isOk() ? parsed.getRef() : ScaipXml.getRefFromRequestBody(body);
        if (parsed.isOk()) {
            String json = ScaipXml.toJsonEcho(parsed);
            return ParsedRequest.valid(ref, json, parsed);
        }
        // Per SCAIP spec: snu=7 only for missing required tags (cid, dty); snu=2 for invalid format
        StatusNumber nackCode = parsed.getReason() != null && parsed.getReason().startsWith("Missing required")
            ? StatusNumber.MANDATORY_TAG_MISSING
            : StatusNumber.INVALID_FORMAT;
        return ParsedRequest.nack(ScaipXml.buildNack(ref, nackCode, parsed.getReason()));
    }

    /**
     * Builds the full SCAIP response body (parse/validate, then if valid: log, forward, ACK/NACK).
     * Package-visible for unit testing without SIP mocks.
     */
    String buildResponseBody(String body, String contentType) {
        ParsedRequest parsed = parseAndValidate(body, contentType);
        if (parsed.hasNack()) {
            return parsed.getNackBody();
        }
        logService.logEvent(parsed.getParseResult());
        boolean forwarded = forwarder == null || forwarder.forward(parsed.getJson());
        if (!forwarded) {
            log.warn("SCAIP message could not be forwarded (ref={}); sending NACK", parsed.getRef());
        }
        return forwarded
            ? ScaipXml.buildAck(parsed.getRef())
            : ScaipXml.buildNack(parsed.getRef(), StatusNumber.NOT_TREATED, "Forward failed");
    }

    private static String getRequestContentType(Request request) {
        javax.sip.header.Header ct = request.getHeader("Content-Type");
        if (ct == null) return null;
        String s = ct.toString();
        int colon = s.indexOf(':');
        return colon >= 0 ? s.substring(colon + 1).trim() : s.trim();
    }

    private static String getBody(Request request) {
        Object content = request.getContent();
        if (content == null) {
            return "";
        }
        if (content instanceof byte[]) {
            return new String((byte[]) content, StandardCharsets.UTF_8);
        }
        return content.toString();
    }

    void setMessageFactory(MessageFactory messageFactory) {
        this.messageFactory = messageFactory;
    }

    void setHeaderFactory(HeaderFactory headerFactory) {
        this.headerFactory = headerFactory;
    }

    @Override
    public void processResponse(ResponseEvent responseEvent) {
        // We only act as UAS; no outgoing client transactions to handle
    }

    @Override
    public void processTimeout(TimeoutEvent timeoutEvent) {
        // No special timeout handling
    }

    @Override
    public void processIOException(IOExceptionEvent exceptionEvent) {
        // Log and continue
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
