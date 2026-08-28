package org.zstack.header.rest;

public final class SyncHttpResponse {
    private final int status;
    private final String body;

    public SyncHttpResponse(int status, String body) {
        this.status = status;
        this.body = body;
    }
    public int getStatus() { return status; }
    public String getBody() { return body; }
}
