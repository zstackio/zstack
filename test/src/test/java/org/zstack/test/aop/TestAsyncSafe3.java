package org.zstack.test.aop;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.test.BeanConstructor;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class TestAsyncSafe3 {
    private static final CLogger logger = Utils.getLogger(TestAsyncSafe3.class);

    private boolean called = false;

    private void throwError(NoErrorCompletion completion) {
        throw new CloudRuntimeException("on purpose");
    }

    @Before
    public void setUp() throws Exception {
        BeanConstructor con = new BeanConstructor();
        con.build();
    }

    @Test
    public void test() {
        throwError(new NoErrorCompletion() {
            @Override
            public void done() {
                called = true;
            }
        });
        Assert.assertTrue(called);
    }

}
