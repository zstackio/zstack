package org.zstack.test.core.workflow;

import junit.framework.Assert;
import org.junit.Test;
import org.zstack.core.workflow.SimpleFlowChain;
import org.zstack.core.workflow.WorkFlowException;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowChain;
import org.zstack.header.core.workflow.FlowDoneHandler;
import org.zstack.header.core.workflow.FlowMarshaller;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;

import java.util.Map;

public class TestSimpleFlow12 {
    boolean success;

    @Test
    public void test() throws WorkFlowException {
        final int[] count = {0};

        new SimpleFlowChain()
                // DEBT: NoRollbackFlow — in test
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
                        // DEBT: NoRollbackFlow — in marshalTheNextFlow
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
