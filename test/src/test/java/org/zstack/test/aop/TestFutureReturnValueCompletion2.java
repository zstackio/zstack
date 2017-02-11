package org.zstack.test.aop;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.FutureReturnValueCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.test.BeanConstructor;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.concurrent.TimeUnit;

/**
 */
public class TestFutureReturnValueCompletion2 {
    CLogger logger = Utils.getLogger(TestFutureReturnValueCompletion2.class);
    ComponentLoader loader;
    CloudBus bus;
    ErrorFacade errf;

    @Before
    public void setUp() throws Exception {
        BeanConstructor con = new BeanConstructor();
        loader = con.build();
        bus = loader.getComponent(CloudBus.class);
        errf = loader.getComponent(ErrorFacade.class);
    }

    private void complete(ReturnValueCompletion completion) {
    }

    @Test
    public void test() throws InterruptedException {
        FutureReturnValueCompletion completion = new FutureReturnValueCompletion(null);
        complete(completion);
        completion.await(TimeUnit.SECONDS.toMillis(1));
        Assert.assertFalse(completion.isSuccess());
        Assert.assertEquals(SysErrors.TIMEOUT.toString(), completion.getErrorCode().getCode());
    }
}
