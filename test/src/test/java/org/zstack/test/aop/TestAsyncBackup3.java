package org.zstack.test.aop;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.MessageReply;
import org.zstack.test.BeanConstructor;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.concurrent.TimeUnit;

/**
 */
public class TestAsyncBackup3 {
    CLogger logger = Utils.getLogger(TestAsyncBackup3.class);
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
        new ChainTask(completion) {
            @Override
            public String getSyncSignature() {
                return null;
            }

            @Override
            public void run(SyncTaskChain chain) {
                throw new RuntimeException("on purpose");
            }

            @Override
            public String getName() {
                return null;
            }
        }.run(null);
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

        TimeUnit.SECONDS.sleep(1);
        Assert.assertTrue(success);
    }

    private void testMethod2(final ReturnValueCompletion completion) {
        new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                throw new RuntimeException("on purpose");
            }
        }.run(null);
    }

    @Test
    public void test1() throws InterruptedException {
        testMethod2(new ReturnValueCompletion(null) {
            @Override
            public void success(Object returnValue) {
            }

            @Override
            public void fail(ErrorCode errorCode) {
                success = true;
                logger.debug(errorCode.toString());
            }
        });

        TimeUnit.SECONDS.sleep(1);
        Assert.assertTrue(success);
    }

}
