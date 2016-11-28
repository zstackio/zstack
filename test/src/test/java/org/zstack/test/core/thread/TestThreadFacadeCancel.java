package org.zstack.test.core.thread;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.thread.Task;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.test.BeanConstructor;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class TestThreadFacadeCancel {
    CLogger logger = Utils.getLogger(TestThreadFacadeCancel.class);
    ComponentLoader loader;
    ThreadFacade thdf;
    int threadNum = 5;
    volatile int count = 0;
    CountDownLatch latch = new CountDownLatch(threadNum - 1);

    class Tester implements Task<Void> {
        int index;

        Tester(int index) {
            this.index = index;
        }

        void sayHello() throws InterruptedException {
            Thread.sleep(500);
            logger.info(String.valueOf(index) + ": " + "hello world");
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
    }

    @Before
    public void setUp() throws Exception {
        BeanConstructor con = new BeanConstructor();
        loader = con.build();
        thdf = loader.getComponent(ThreadFacade.class);
    }

    @Test
    public void test() throws InterruptedException {
        for (int i = 0; i < threadNum; i++) {
            Tester worker = new Tester(i);
            Future<Void> f = thdf.submit(worker);
            if (i == threadNum - 1) {
                f.cancel(true);
            }
        }
        latch.await(10, TimeUnit.SECONDS);
        Assert.assertEquals(threadNum - 1, this.count);
    }
}
