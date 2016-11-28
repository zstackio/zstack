package org.zstack.test.core.workflow;

import junit.framework.Assert;
import org.junit.Test;
import org.zstack.core.workflow.SimpleFlowChain;
import org.zstack.core.workflow.WorkFlowException;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 3:33 PM
 * To change this template use File | Settings | File Templates.
 */
public class TestSimpleFlow5 {
    CLogger logger = Utils.getLogger(TestSimpleFlow5.class);
    boolean success;

    @Test
    public void test() throws WorkFlowException {

        Map<String, Object> data = new HashMap<String, Object>();
        new SimpleFlowChain()
                .setData(data)
                .then(new NoRollbackFlow() {
                    @Override
                    public void run(FlowTrigger chain, Map data) {
                        data.put("value", 1);
                        chain.next();
                    }
                })
                .then(new NoRollbackFlow() {
                    @Override
                    public void run(FlowTrigger chain, Map data) {
                        Integer ret = (Integer) data.get("value");
                        success = ret == 1;
                    }
                })
                .start();

        Assert.assertTrue(success);
    }
}
