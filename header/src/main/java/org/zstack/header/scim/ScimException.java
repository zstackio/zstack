package org.zstack.header.scim;

public class ScimException extends RuntimeException {
    private final int statusCode;

    public ScimException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
