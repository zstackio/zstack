package org.zstack.core.thread;

import org.zstack.header.core.AbstractCompletion;
import org.zstack.header.core.AsyncBackup;

public abstract class ChainTask extends AbstractCompletion {
    public ChainTask(AsyncBackup one, AsyncBackup...others) {
        super(one, others);
    }

    public abstract String getSyncSignature();

    public abstract void run(SyncTaskChain chain);

    public abstract String getName();

    protected int getSyncLevel() {
        return 1;
    }
}
