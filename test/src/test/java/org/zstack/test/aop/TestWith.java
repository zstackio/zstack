package org.zstack.test.aop;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.With;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.test.BeanConstructor;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/**
 */
public class TestWith {
    CLogger logger = Utils.getLogger(TestWith.class);
    boolean success;
    ComponentLoader loader;
    CloudBus bus;

    @Before
    public void setUp() throws Exception {
        BeanConstructor con = new BeanConstructor();
        loader = con.build();
        bus = loader.getComponent(CloudBus.class);
    }

    private void testMethod(final Completion completion) {
        new With(completion).run(new Runnable() {
            @Override
            public void run() {
                throw new RuntimeException("on purpose");
            }
        });
    }

    @Test
    public void test() throws InterruptedException {
        testMethod(new Completion(null) {
            @Override
            public void success() {
            }

            @Override
            public void fail(ErrorCode errorCode) {
                success = true;
                logger.debug(errorCode.toString());
            }
        });

        Assert.assertTrue(success);
    }

}
