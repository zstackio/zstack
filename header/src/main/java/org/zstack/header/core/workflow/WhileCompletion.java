package org.zstack.header.core.workflow;

import org.zstack.header.core.AsyncBackup;
import org.zstack.header.core.NoErrorCompletion;

/**
 * Created by Administrator on 2017-05-12.
 */
public abstract class WhileCompletion extends NoErrorCompletion {
    public WhileCompletion(AsyncBackup... completion) {
        super(completion);
    }
    public abstract void allDone();
}
