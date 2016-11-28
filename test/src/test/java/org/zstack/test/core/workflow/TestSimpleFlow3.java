package org.zstack.test.core.workflow;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.workflow.SimpleFlowChain;
import org.zstack.core.workflow.WorkFlowException;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.test.BeanConstructor;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 3:33 PM
 * To change this template use File | Settings | File Templates.
 */
public class TestSimpleFlow3 {
    CLogger logger = Utils.getLogger(TestSimpleFlow3.class);

    @Before
    public void setUp() throws Exception {
        new BeanConstructor().build();
    }

    @Test
    public void test() throws WorkFlowException {
        final int[] count = {0};

        new SimpleFlowChain()
                .then(new Flow() {
                    @Override
                    public void run(FlowTrigger chain, Map data) {
                        count[0]++;
                        chain.next();
                    }

                    @Override
                    public void rollback(FlowRollback chain, Map data) {
                        count[0]--;
                        chain.rollback();
                    }
                })
                .then(new Flow() {
                    @Override
                    public void run(FlowTrigger chain, Map data) {
                        count[0]++;
                        chain.next();
                    }

                    @Override
                    public void rollback(FlowRollback chain, Map data) {
                        count[0]--;
                        chain.rollback();
                    }
                })
                .then(new Flow() {
                    boolean s = false;

                    @Override
                    public void run(FlowTrigger chain, Map data) {
                        throw new RuntimeException("on purpose");
                    }

                    @Override
                    public void rollback(FlowRollback chain, Map data) {
                        if (s) {
                            count[0]--;
                        }
                        chain.rollback();
                    }
                })
                .start();

        Assert.assertEquals(0, count[0]);
    }
}
