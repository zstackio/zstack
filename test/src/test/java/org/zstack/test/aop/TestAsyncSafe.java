package org.zstack.test.aop;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.test.BeanConstructor;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class TestAsyncSafe {
    private static final CLogger logger = Utils.getLogger(TestAsyncSafe.class);

    private boolean called = false;

    ComponentLoader loader;
    CloudBus bus;

    @Before
    public void setUp() throws Exception {
        BeanConstructor con = new BeanConstructor();
        loader = con.build();
        bus = loader.getComponent(CloudBus.class);
    }

    private void throwError(Completion complete) {
        throw new CloudRuntimeException("on purpose");
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
                called = true;
            }
        });

        Assert.assertTrue(called);
    }

}
