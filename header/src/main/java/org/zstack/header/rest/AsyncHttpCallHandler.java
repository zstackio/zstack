package org.zstack.header.rest;

import org.zstack.header.core.ReturnValueCompletion;

/**
 * Created by frank on 11/1/2015.
 */
public interface AsyncHttpCallHandler<T> extends HttpCallHandler {
    void handleAsyncHttpCall(T object, ReturnValueCompletion<String> completion);
}
