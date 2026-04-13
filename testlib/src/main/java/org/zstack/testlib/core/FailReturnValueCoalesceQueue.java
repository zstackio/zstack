package org.zstack.testlib.core;

import org.zstack.core.thread.ReturnValueCoalesceQueue;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.OperationFailureException;

import java.util.List;

import static org.zstack.core.Platform.operr;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_CORE_THREAD_10004;

public class FailReturnValueCoalesceQueue extends ReturnValueCoalesceQueue<Integer, String, String> {
    @Override
    protected String getName() {
        return "test-rv-failure";
    }

    @Override
    protected void executeBatch(List<Integer> items, ReturnValueCompletion<String> completion) {
        throw new OperationFailureException(operr(ORG_ZSTACK_CORE_THREAD_10004, "test rv error"));
    }

    @Override
    protected String calculateResult(Integer item, String batchResult) {
        return null;
    }
}
