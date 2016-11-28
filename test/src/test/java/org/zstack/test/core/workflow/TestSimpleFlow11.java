package org.zstack.test.core.workflow;

import junit.framework.Assert;
import org.junit.Test;
import org.zstack.core.workflow.WorkFlowException;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.test.BeanConstructor;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 3:33 PM
 * To change this template use File | Settings | File Templates.
 */
public class TestSimpleFlow11 {
    CLogger logger = Utils.getLogger(TestSimpleFlow11.class);
    boolean s = false;

    @Test
    public void test() throws WorkFlowException {
        new BeanConstructor().build();


        FlowRollback rollback = new FlowRollback() {
            @Override
            public void rollback() {
                s = true;
            }

            @Override
            public void skipRestRollbacks() {

            }
        };

        new Completion(rollback) {
            @Override
            public void success() {

            }

            @Override
            public void fail(ErrorCode errorCode) {
                throw new RuntimeException("on purpose");
            }
        }.fail(null);


        Assert.assertTrue(s);

        s = false;

        rollback = new FlowRollback() {
            @Override
            public void rollback() {
                s = true;
            }

            @Override
            public void skipRestRollbacks() {

            }
        };

        new Completion(rollback) {
            @Override
            public void success() {
                throw new CloudRuntimeException("on purpose");
            }

            @Override
            public void fail(ErrorCode errorCode) {

            }
        }.success();


        Assert.assertTrue(s);

        s = false;


        FlowTrigger trigger = new FlowTrigger() {
            @Override
            public void fail(ErrorCode errorCode) {
                s = true;
            }

            @Override
            public void next() {

            }

            @Override
            public void setError(ErrorCode error) {

            }
        };

        new Completion(trigger) {
            @Override
            public void success() {
                throw new RuntimeException("on purpose");
            }

            @Override
            public void fail(ErrorCode errorCode) {
            }
        }.success();


        Assert.assertTrue(s);

        s = false;

        trigger = new FlowTrigger() {
            @Override
            public void fail(ErrorCode errorCode) {
                s = true;
            }

            @Override
            public void next() {

            }

            @Override
            public void setError(ErrorCode error) {

            }
        };

        new Completion(trigger) {
            @Override
            public void success() {

            }

            @Override
            public void fail(ErrorCode errorCode) {
                throw new RuntimeException("on purpose");
            }
        }.fail(null);

        Assert.assertTrue(s);
    }
}
