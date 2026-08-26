package org.zstack.header.rest;

public interface SyncHttpStatusBodyCallHandler<T> extends HttpCallHandler {
    SyncHttpResponse handleSyncHttpCall(T cmd);
}
