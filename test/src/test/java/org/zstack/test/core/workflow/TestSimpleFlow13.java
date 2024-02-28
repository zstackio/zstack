package org.zstack.test.core.workflow;

import org.junit.Assert;
import org.junit.Test;
import org.zstack.core.workflow.SimpleFlowChain;
import org.zstack.core.workflow.WorkFlowException;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;

import java.util.Map;

import static org.zstack.core.Platform.operr;


/*
    test skipped flow will not roll back
 */
public class TestSimpleFlow13 {
    @Test
    //skipped flow will not roll back
    public void test() throws WorkFlowException {
        final int[] count = {0};

        new SimpleFlowChain().then(new NoRollbackFlow() {
            final String __name__ = "flow1_skip";
            @Override
            public void run(FlowTrigger chain, Map data) {
                count[0] = count[0] + 1;
                chain.next();
            }

            @Override
            public boolean skip(Map data) {
                return true;
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                count[0] = count[0] - 1;
                trigger.rollback();
            }
        }).then(new NoRollbackFlow() {
            final String __name__ = "flow2";

            @Override
            public void run(FlowTrigger chain, Map data) {
                count[0] = count[0] + 1;
                chain.next();
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                count[0] = count[0] - 1;
                trigger.rollback();
            }
        }).then(new NoRollbackFlow() {
            final String __name__ = "flow3_error";
            @Override
            public void run(FlowTrigger chain, Map data) {
                count[0] = count[0] + 1;
                throw new OperationFailureException(operr("on purpose"));
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                count[0] = count[0] - 1;
                trigger.rollback();
            }
        }).done(new FlowDoneHandler(null) {
            @Override
            public void handle(Map data) {
            }
        }).error(new FlowErrorHandler(null) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
            }
        }).start();

        Assert.assertEquals("count == 0", 0, count[0]);
    }
}

