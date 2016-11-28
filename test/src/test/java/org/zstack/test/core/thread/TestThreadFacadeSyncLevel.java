package org.zstack.test.core.thread;

import org.junit.Before;
import org.junit.Test;
import org.zstack.core.thread.SyncThread;
import org.zstack.test.BeanConstructor;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


public class TestThreadFacadeSyncLevel {
    private static final CLogger logger = Utils.getLogger(TestThreadFacadeSyncLevel.class);
    private Map<String, String> thredNames = new HashMap<String, String>();
    int taskNums = 500;
    CountDownLatch latch = new CountDownLatch(taskNums);

    class Tester {
        int index;

        Tester(int index) {
            this.index = index;
        }

        @SyncThread(level = 300)
        void sayHello() throws InterruptedException {
            String name = Thread.currentThread().getName();
            thredNames.put(name, name);
            logger.info(String.valueOf(index) + ": " + "hello world");
            TimeUnit.SECONDS.sleep(10);
            latch.countDown();
        }
    }

    @Before
    public void setUp() throws Exception {
        BeanConstructor con = new BeanConstructor();
        /* This loads spring application context */
        con.build();
    }

    @Test
    public void test() throws InterruptedException {
        for (int i = 0; i < taskNums; i++) {
            Tester worker = new Tester(i);
            worker.sayHello();
        }
        latch.await(60, TimeUnit.SECONDS);
        //Assert.assertEquals(5, thredNames.size());
    }
}
