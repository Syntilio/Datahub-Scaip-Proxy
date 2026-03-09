package com.syntilio.scaip.server;

import com.syntilio.scaip.domain.ScaipXml;

/**
 * Result of parsing and validating the request (content-type + body).
 * Either a NACK body (content-type or parse failure) or valid content (ref, json, parseResult).
 */
final class ParsedRequest {
    private final String nackBody;
    private final String ref;
    private final String json;
    private final ScaipXml.ParseResult parseResult;

    static ParsedRequest nack(String nackBody) {
        return new ParsedRequest(nackBody, null, null, null);
    }

    static ParsedRequest valid(String ref, String json, ScaipXml.ParseResult parseResult) {
        return new ParsedRequest(null, ref, json, parseResult);
    }

    private ParsedRequest(String nackBody, String ref, String json, ScaipXml.ParseResult parseResult) {
        this.nackBody = nackBody;
        this.ref = ref;
        this.json = json;
        this.parseResult = parseResult;
    }

    boolean hasNack() {
        return nackBody != null;
    }

    String getNackBody() {
        return nackBody;
    }

    String getRef() {
        return ref;
    }

    String getJson() {
        return json;
    }

    ScaipXml.ParseResult getParseResult() {
        return parseResult;
    }
}