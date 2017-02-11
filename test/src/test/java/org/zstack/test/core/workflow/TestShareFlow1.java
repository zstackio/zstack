package org.zstack.test.core.workflow;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.test.BeanConstructor;

import java.util.Map;

/**
 */
public class TestShareFlow1 {
    int[] count = {0};
    boolean success;

    private void increase() {
        count[0]++;
    }

    private void decrease() {
        count[0]--;
    }

    private void expect(int ret) {
        Assert.assertEquals(ret, count[0]);
    }

    @Test
    public void test() {
        FlowChain chain = FlowChainBuilder.newShareFlowChain();

        chain.then(new ShareFlow() {
            int a;

            @Override
            public void setup() {
                flow(new Flow() {
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        a = 1;
                        increase();
                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        decrease();
                        trigger.rollback();
                    }
                });

                flow(new Flow() {
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        a = 2;
                        increase();
                        throw new RuntimeException("on purpose");
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        decrease();
                        trigger.rollback();
                    }
                });
            }
        }).error(new FlowErrorHandler(null) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                success = true;
            }
        }).start();

        Assert.assertTrue(success);
        expect(0);
    }


    @Before
    public void setUp() throws Exception {
        new BeanConstructor().build();
    }
}
