package org.zstack.test.aop;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.thread.AsyncThread;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.test.BeanConstructor;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.concurrent.TimeUnit;

public class TestAsyncBackup {
    CLogger logger = Utils.getLogger(TestAsyncBackup.class);
    boolean success;
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
        Completion completion = new Completion(null) {
            @Override
            public void success() {
            }

            @Override
            public void fail(ErrorCode errorCode) {
                success = true;
                logger.debug(errorCode.toString());
            }
        };

        try {
            new Completion(completion) {
                @Override
                @AsyncThread
                public void success() {
                    throw new RuntimeException("on purpose");
                }

                @Override
                public void fail(ErrorCode errorCode) {

                }
            }.success();
        } catch (RuntimeException e) {
            //pass
        }


        TimeUnit.SECONDS.sleep(1);
        Assert.assertTrue(success);
    }

}
