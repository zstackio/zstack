package org.zstack.externalStorage.sdk;

public class ExternalStorageApiException extends RuntimeException {
    public ExternalStorageApiException(String format) {
        super(format);
    }

    public ExternalStorageApiException(Exception e) {
        super(e);
    }
}
