package org.zstack.test.aop;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.thread.AsyncThread;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.rest.JsonAsyncRESTCallback;
import org.zstack.test.BeanConstructor;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.concurrent.TimeUnit;

/**
 */
public class TestAsyncBackup4 {
    CLogger logger = Utils.getLogger(TestAsyncBackup4.class);
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
        new JsonAsyncRESTCallback<Void>(completion) {
            @Override
            public void fail(ErrorCode err) {

            }

            @Override
            @AsyncThread
            public void success(Void ret) {
                throw new RuntimeException("on purpose");
            }

            @Override
            public Class<Void> getReturnClass() {
                return Void.class;
            }
        }.success((Void) null);
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

    private void testMethod1(final Completion completion) {
        new JsonAsyncRESTCallback<Void>(completion) {
            @Override
            public void fail(ErrorCode err) {

            }

            @AsyncThread
            private void timeout(long timeout) {
                throw new RuntimeException("on purpose");
            }

            @Override
            public void success(Void ret) {
            }

            @Override
            public Class<Void> getReturnClass() {
                return Void.class;
            }
        }.timeout(0);
    }

    @Test
    public void test1() throws InterruptedException {
        testMethod1(new Completion(null) {
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

    private void testMethod2(final Completion completion) {
        new JsonAsyncRESTCallback<Void>(completion) {
            @Override
            @AsyncThread
            public void fail(ErrorCode err) {
                throw new RuntimeException("on purpose");
            }

            @Override
            public void success(Void ret) {
            }

            @Override
            public Class<Void> getReturnClass() {
                return Void.class;
            }
        }.fail(null);
    }

    @Test
    public void test2() throws InterruptedException {
        success = false;
        testMethod2(new Completion(null) {
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

}
