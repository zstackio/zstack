package org.zstack.xinfini.sdk;

public class XInfiniApiException extends RuntimeException {
    public XInfiniApiException(String format) {
        super(format);
    }

    public XInfiniApiException(Exception e) {
        super(e);
    }
}
