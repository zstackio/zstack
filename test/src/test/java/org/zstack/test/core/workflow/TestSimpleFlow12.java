package org.zstack.test.core.workflow;

import junit.framework.Assert;
import org.junit.Test;
import org.zstack.core.workflow.SimpleFlowChain;
import org.zstack.core.workflow.WorkFlowException;
import org.zstack.header.core.workflow.*;

import java.util.Map;

public class TestSimpleFlow12 {
    boolean success;

    @Test
    public void test() throws WorkFlowException {
        final int[] count = {0};

        new SimpleFlowChain()
                .then(new NoRollbackFlow() {
                    @Override
                    public void run(FlowTrigger chain, Map data) {
                        count[0] = 100;
                        chain.next();
                    }

                    @Override
                    public boolean skip(Map data) {
                        return true;
                    }
                })
                .setFlowMarshaller(new FlowMarshaller() {
                    @Override
                    public Flow marshalTheNextFlow(String previousFlowClassName, String nextFlowClassName, FlowChain chain, Map data) {
                        return new NoRollbackFlow() {
                            @Override
                            public void run(FlowTrigger trigger, Map data) {
                                count[0] = -100;
                                trigger.next();
                            }
                        };
                    }
                })
                .done(new FlowDoneHandler(null) {
                    @Override
                    public void handle(Map data) {
                        success = count[0] == -100;
                    }
                })
                .start();

        Assert.assertTrue(String.format("success = %s", success), success);
    }
}
