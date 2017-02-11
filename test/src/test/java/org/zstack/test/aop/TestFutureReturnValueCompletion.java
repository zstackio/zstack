package org.zstack.test.aop;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.header.core.FutureReturnValueCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.test.BeanConstructor;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/**
 */
public class TestFutureReturnValueCompletion {
    CLogger logger = Utils.getLogger(TestFutureReturnValueCompletion.class);
    ComponentLoader loader;
    CloudBus bus;

    @Before
    public void setUp() throws Exception {
        BeanConstructor con = new BeanConstructor();
        loader = con.build();
        bus = loader.getComponent(CloudBus.class);
    }

    private void complete(ReturnValueCompletion completion) {
        completion.success(1);
    }

    @Test
    public void test() throws InterruptedException {
        FutureReturnValueCompletion completion = new FutureReturnValueCompletion(null);
        complete(completion);
        completion.await();
        Assert.assertTrue(completion.isSuccess());
        Integer val = completion.getResult();
        Assert.assertEquals(1, val.intValue());
    }
}
