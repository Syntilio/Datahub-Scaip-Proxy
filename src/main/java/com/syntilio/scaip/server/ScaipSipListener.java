package com.syntilio.scaip.server;

import com.syntilio.scaip.ScaipXml;
import com.syntilio.scaip.enums.StatusNumber;

import javax.sip.*;
import javax.sip.header.ContentTypeHeader;
import javax.sip.header.HeaderFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;
import javax.sip.message.MessageFactory;
import java.nio.charset.StandardCharsets;

public class ScaipSipListener implements SipListener {

    private final LogService logService = new LogService();
    private MessageFactory messageFactory;
    private HeaderFactory headerFactory;

    void setMessageFactory(MessageFactory messageFactory) {
        this.messageFactory = messageFactory;
    }

    void setHeaderFactory(HeaderFactory headerFactory) {
        this.headerFactory = headerFactory;
    }

    @Override
    public void processRequest(RequestEvent requestEvent) {
        Request request = requestEvent.getRequest();
        if (!Request.MESSAGE.equals(request.getMethod())) {
            return;
        }

        String body = getBody(request);
        logService.logIncoming(body != null ? body : "");

        String responseBody;
        String contentType = getRequestContentType(request);
        if (!ScaipXml.CONTENT_TYPE.equalsIgnoreCase(contentType != null ? contentType.trim() : "")) {
            responseBody = ScaipXml.buildNack(null, StatusNumber.INVALID_FORMAT, "Content-Type must be application/xml");
        } else {
            ScaipXml.ParseResult parsed = ScaipXml.parseRequest(body);
            String ref = parsed.isOk() ? parsed.getRef() : ScaipXml.getRefFromRequestBody(body);
            if (parsed.isOk()) {
                logService.logEvent(parsed);
                responseBody = ScaipXml.buildAck(ref);
            } else {
                responseBody = ScaipXml.buildNack(ref, StatusNumber.MANDATORY_TAG_MISSING, parsed.getReason());
            }
        }

        try {
            ServerTransaction serverTransaction = requestEvent.getServerTransaction();
            if (serverTransaction == null) {
                SipProvider provider = (SipProvider) requestEvent.getSource();
                serverTransaction = provider.getNewServerTransaction(request);
            }
            Response response = messageFactory.createResponse(Response.OK, request);
            byte[] bodyBytes = responseBody.getBytes(StandardCharsets.UTF_8);
            ContentTypeHeader ctHeader = headerFactory.createContentTypeHeader("application", "xml");
            response.setContent(bodyBytes, ctHeader);
            serverTransaction.sendResponse(response);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send SIP 200 OK", e);
        }
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
