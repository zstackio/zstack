package org.zstack.header.core;

import org.zstack.header.errorcode.ErrorCode;

/**
 */
public class NopeReturnValueCompletion extends ReturnValueCompletion {
    public NopeReturnValueCompletion(AsyncBackup...completion) {
        super(completion);
    }

    public NopeReturnValueCompletion() {
        super();
    }

    @Override
    public void success(Object returnValue) {
    }

    @Override
    public void fail(ErrorCode errorCode) {
    }
}
