package org.zstack.core.gc;

import org.zstack.header.errorcode.ErrorCode;

/**
 * Created by frank on 8/5/2015.
 */
public interface GCCompletion {
    void success();

    void fail(ErrorCode errorCode);

    void cancel();
}
