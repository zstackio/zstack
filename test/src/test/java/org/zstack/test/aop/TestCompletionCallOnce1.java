package org.zstack.test.aop;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.test.BeanConstructor;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/**
 */
public class TestCompletionCallOnce1 {
    CLogger logger = Utils.getLogger(TestCompletionCallOnce1.class);
    int success = 0;
    int fail = 0;
    ComponentLoader loader;
    CloudBus bus;

    @Before
    public void setUp() throws Exception {
        BeanConstructor con = new BeanConstructor();
        loader = con.build();
        bus = loader.getComponent(CloudBus.class);
    }

    @Test
    public void test() throws InterruptedException {
        ReturnValueCompletion<Void> completion = new ReturnValueCompletion<Void>(null) {
            @Override
            public void success(Void returnValue) {
                success++;
            }

            @Override
            public void fail(ErrorCode errorCode) {
                fail++;
            }
        };

        completion.success(null);
        completion.success(null);
        completion.fail(null);
        completion.fail(null);

        Assert.assertEquals(1, success);
        Assert.assertEquals(1, fail);
    }

}
