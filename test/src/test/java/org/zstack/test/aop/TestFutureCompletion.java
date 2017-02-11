package org.zstack.test.aop;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.header.core.Completion;
import org.zstack.header.core.FutureCompletion;
import org.zstack.test.BeanConstructor;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/**
 */
public class TestFutureCompletion {
    CLogger logger = Utils.getLogger(TestFutureCompletion.class);
    boolean success;
    ComponentLoader loader;
    CloudBus bus;

    @Before
    public void setUp() throws Exception {
        BeanConstructor con = new BeanConstructor();
        loader = con.build();
        bus = loader.getComponent(CloudBus.class);
    }

    private void complete(Completion completion) {
        success = true;
        completion.success();
    }

    @Test
    public void test() throws InterruptedException {
        FutureCompletion completion = new FutureCompletion(null);
        complete(completion);
        completion.await();
        Assert.assertTrue(success);
        Assert.assertTrue(completion.isSuccess());
    }
}
