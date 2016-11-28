package org.zstack.test.core.thread;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.thread.SyncTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.test.BeanConstructor;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;


public class TestThreadFacadeCancelSyncThread {
    private static final CLogger logger = Utils.getLogger(TestThreadFacadeCancelSyncThread.class);
    ComponentLoader loader;
    ThreadFacade thdf;
    int threadNum = 5;
    volatile int count = 0;
    CountDownLatch latch = new CountDownLatch(threadNum - 1);

    class Tester implements SyncTask<Void> {
        int index;

        Tester(int index) {
            this.index = index;
        }

        void sayHello() throws InterruptedException {
            logger.info(String.valueOf(index) + ": " + "hello world");
            Thread.sleep(500);
            count++;
            latch.countDown();
        }

        @Override
        public Void call() throws Exception {
            sayHello();
            return null;
        }

        @Override
        public String getName() {
            return "Tester";
        }

        @Override
        public String getSyncSignature() {
            return "sync";
        }

        @Override
        public int getSyncLevel() {
            return 1;
        }
    }

    @Before
    public void setUp() throws Exception {
        BeanConstructor con = new BeanConstructor();
        loader = con.build();
        thdf = loader.getComponent(ThreadFacade.class);
    }

    @Test
    public void test() throws InterruptedException {
        for (int i = 0; i < this.threadNum; i++) {
            Tester worker = new Tester(i);
            Future<Void> f = thdf.syncSubmit(worker);
            if (i == this.threadNum - 1) {
                f.cancel(true);
            }
        }
        latch.await(10, TimeUnit.SECONDS);
        Assert.assertEquals(threadNum - 1, this.count);
    }
}
