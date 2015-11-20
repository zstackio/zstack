package org.zstack.test.core.workflow;

import junit.framework.Assert;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.workflow.SimpleFlowChain;
import org.zstack.core.workflow.WorkFlowException;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.exception.CloudRuntimeException;
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
public class TestSimpleFlow11 {
    CLogger logger = Utils.getLogger(TestSimpleFlow11.class);
    boolean success;

    @Test
    public void test() throws WorkFlowException {
        BeanConstructor con = new BeanConstructor();
        con.build();

        try {
            new SimpleFlowChain()
                    .then(new NoRollbackFlow() {
                        @Override
                        public void run(FlowTrigger chain, Map data) {
                            chain.next();
                        }
                    })
                    .then(new NoRollbackFlow() {
                        @Override
                        public void run(FlowTrigger chain, Map data) {
                            chain.rollback();
                        }
                    })
                    .start();
        } catch (CloudRuntimeException e) {
            success = true;
        }

        Assert.assertTrue(success);
    }
}
