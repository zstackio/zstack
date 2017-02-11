package org.zstack.test.aop;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.FlowErrorHandler;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.test.BeanConstructor;

import java.util.Map;

/**
 */
public class TestAsyncBackup10 {
    boolean success;

    @Test
    public void test() throws InterruptedException {
        Completion completion = new Completion(null) {
            @Override
            public void success() {
            }

            @Override
            public void fail(ErrorCode errorCode) {
                success = true;
            }
        };

        FlowErrorHandler errorHandler = new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                throw new CloudRuntimeException("error");
            }
        };


        try {
            errorHandler.handle(null, null);
        } catch (CloudRuntimeException e) {
            //pass
        }

        Assert.assertTrue(success);
    }


    @Before
    public void setUp() throws Exception {
        new BeanConstructor().build();
    }
}
