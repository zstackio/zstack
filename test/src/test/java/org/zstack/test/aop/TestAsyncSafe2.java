package org.zstack.test.aop;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.test.BeanConstructor;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class TestAsyncSafe2 {
    private static final CLogger logger = Utils.getLogger(TestAsyncSafe2.class);

    private boolean called = false;
    private boolean called1 = false;

    private void throwError(Completion complete1, ReturnValueCompletion complete) {
        throw new CloudRuntimeException("on purpose");
    }

    @Before
    public void setUp() throws Exception {
        BeanConstructor con = new BeanConstructor();
        con.build();
    }

    @Test
    public void test() {
        throwError(new Completion(null) {
            @Override
            public void success() {
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.debug(errorCode.toString());
                called1 = true;
            }
        }, new ReturnValueCompletion(null) {
            @Override
            public void fail(ErrorCode errorCode) {
                logger.debug(errorCode.toString());
                called = true;
            }

            @Override
            public void success(Object returnValue) {
            }
        });

        Assert.assertTrue(called);
        Assert.assertTrue(called1);
    }

}
