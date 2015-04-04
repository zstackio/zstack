package org.zstack.header.core;

import org.zstack.header.errorcode.ErrorCode;

public abstract class ReturnValueCompletion<T> extends AbstractCompletion {
    public ReturnValueCompletion(AsyncBackup...completion) {
        super(completion);
    }

    public ReturnValueCompletion() {
        super();
    }

    public abstract void success(T returnValue);
    
    public abstract void fail(ErrorCode errorCode);
}
