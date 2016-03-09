package org.zstack.header.core;

import org.zstack.header.errorcode.ErrorCode;

public abstract class Completion extends AbstractCompletion {
    public Completion(AsyncBackup...backup) {
        super(backup);
    }

    public Completion() {
        super();
    }

    public abstract void success();
    
    public abstract void fail(ErrorCode errorCode);
}
