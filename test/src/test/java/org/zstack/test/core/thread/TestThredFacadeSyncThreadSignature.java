package org.zstack.test.core.thread;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.thread.SyncThread;
import org.zstack.core.thread.SyncThreadSignature;
import org.zstack.test.BeanConstructor;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


public class TestThredFacadeSyncThreadSignature {
    private static final CLogger logger = Utils.getLogger(TestThredFacadeSyncThreadSignature.class);
    int taskNum = 100;
    List<Integer> helloList = new ArrayList<Integer>(taskNum);
    List<Integer> byeList = new ArrayList<Integer>(taskNum);
    List<Integer> okList = new ArrayList<Integer>(taskNum);
    CountDownLatch latch = new CountDownLatch(3);

    class Tester implements SyncThreadSignature {
        int index;

        Tester(int index) {
            this.index = index;
        }

        @SyncThread(compoundSignature = true)
        void sayHello() {
            logger.info(String.valueOf(index) + ": " + "hello world");
            helloList.add(index);
            if (index == taskNum - 1) {
                latch.countDown();
            }
        }

        @SyncThread(compoundSignature = true, signature = "bye")
        void sayBye() {
            logger.info(String.valueOf(index) + ": " + "byte");
            byeList.add(index);
            if (index == taskNum - 1) {
                latch.countDown();
            }
        }

        @SyncThread
        void sayOk() {
            logger.info(String.valueOf(index) + ": " + "ok");
            okList.add(index);
            if (index == taskNum - 1) {
                latch.countDown();
            }
        }

        void sayAll() {
            sayHello();
            sayBye();
            sayOk();
        }

        @Override
        public String getSyncSignature() {
            return "Tester";
        }
    }

    @Before
    public void setUp() throws Exception {
        BeanConstructor con = new BeanConstructor();
        /* This loads spring application context */
        con.build();
    }

    private void assertIfListIsNotOrdered(List<Integer> list) {
        Assert.assertEquals(taskNum, list.size());
        int num = -1;
        for (Integer i : list) {
            if (i <= num) {
                Assert.fail();
            }
            num = i;
        }
    }

    @Test
    public void test() throws InterruptedException {
        for (int i = 0; i < this.taskNum; i++) {
            Tester worker = new Tester(i);
            worker.sayAll();
        }
        latch.await(10, TimeUnit.SECONDS);
        assertIfListIsNotOrdered(helloList);
        assertIfListIsNotOrdered(okList);
        assertIfListIsNotOrdered(byeList);
    }
}
