package org.zstack.test.core.thread;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.thread.AsyncThread;
import org.zstack.core.thread.ScheduledThread;
import org.zstack.core.thread.SyncThread;
import org.zstack.test.BeanConstructor;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class TestThreadFacadeAnnotationFuture {
    private static final CLogger logger = Utils.getLogger(TestThreadFacadeAnnotationFuture.class);
    ComponentLoader loader;
    int count = 0;

    @Before
    public void setUp() throws Exception {
        BeanConstructor con = new BeanConstructor();
        /* This loads spring application context */
        con.build();
    }

    @SyncThread
    Future<Void> test1() {
        try {
            Thread.sleep(500);
            count++;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        logger.info("test1");
        return null;
    }


    @AsyncThread
    Future<Void> test2() {
        try {
            Thread.sleep(500);
            count++;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        logger.info("test2");
        return null;
    }

    @ScheduledThread(interval = 1)
    Future<Void> test3() {
        logger.info("test3");
        return null;
    }

    @Test
    public void test() throws InterruptedException, ExecutionException {
        Future<Void> f = test1();
        f.get();
        Assert.assertEquals(count, 1);
        f = test2();
        f.get();
        Assert.assertEquals(count, 2);
        f = test3();
        Thread.sleep(1000);
        f.cancel(true);
    }

}
