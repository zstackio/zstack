package org.zstack.header.core;

import org.zstack.header.errorcode.ErrorCode;

/**
 * Created by xing5 on 2016/3/29.
 */
public abstract class HaCheckerCompletion extends AbstractCompletion {
    public HaCheckerCompletion(AsyncBackup one, AsyncBackup... others) {
        super(one, others);
    }

    public abstract void success();

    public abstract void fail(ErrorCode errorCode);

    public abstract void noWay();

    public abstract void notStable();
}
