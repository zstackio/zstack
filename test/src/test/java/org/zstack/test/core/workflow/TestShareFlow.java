package org.zstack.test.core.workflow;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.workflow.FlowChain;
import org.zstack.header.core.workflow.FlowDoneHandler;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.test.BeanConstructor;

import java.util.Map;

/**
 */
public class TestShareFlow {
    int[] count = {0};
    boolean success;

    private void increase() {
        count[0]++;
    }

    private void decrease() {
        count[0]--;
    }

    private void expect(int ret) {
        Assert.assertEquals(count[0], ret);
    }

    @Test
    public void test() {
        FlowChain chain = FlowChainBuilder.newShareFlowChain();

        chain.then(new ShareFlow() {
            int a;

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        a = 1;
                        increase();
                        trigger.next();
                    }
                });

                flow(new NoRollbackFlow() {
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        a = 2;
                        increase();
                        trigger.next();
                    }
                });
            }
        }).done(new FlowDoneHandler(null) {
            @Override
            public void handle(Map data) {
                success = true;
            }
        }).start();

        Assert.assertTrue(success);
        expect(2);
    }


    @Before
    public void setUp() throws Exception {
        new BeanConstructor().build();
    }
}
