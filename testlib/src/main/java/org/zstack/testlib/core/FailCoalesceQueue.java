package org.zstack.testlib.core;

import org.zstack.core.thread.CoalesceQueue;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.OperationFailureException;

import java.util.List;

import static org.zstack.core.Platform.operr;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_CORE_THREAD_10004;

public class FailCoalesceQueue extends CoalesceQueue<Integer> {
    @Override
    protected String getName() {
        return "test-failure";
    }

    @Override
    protected void executeBatch(List<Integer> items, Completion completion) {
        throw new OperationFailureException(operr(ORG_ZSTACK_CORE_THREAD_10004, "test error"));
    }
}
