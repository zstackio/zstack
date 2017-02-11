package org.zstack.test.core.thread;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.thread.AsyncThread;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.test.BeanConstructor;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class TestChainTask2 {
    CLogger logger = Utils.getLogger(TestChainTask2.class);
    ComponentLoader loader;
    ThreadFacade thdf;
    AtomicInteger threadNum = new AtomicInteger(1000);
    List<Integer> res = new ArrayList<Integer>(threadNum.get());
    List<Integer> f1 = new ArrayList<Integer>(threadNum.get());
    List<Integer> f2 = new ArrayList<Integer>(threadNum.get());
    CountDownLatch latch = new CountDownLatch(threadNum.get());

    class Tester extends ChainTask {
        int index;

        Tester(int index) {
            super(null);
            this.index = index;
        }

        @Override
        public String getName() {
            return "Test";
        }

        @Override
        public String getSyncSignature() {
            return "Test";
        }

        @Override
        public void run(SyncTaskChain chain) {
            logger.debug(String.valueOf(index));
            res.add(index);
            latch.countDown();
            chain.next();
        }
    }

    @Before
    public void setUp() throws Exception {
        BeanConstructor con = new BeanConstructor();
        loader = con.build();
        thdf = loader.getComponent(ThreadFacade.class);
    }

    @AsyncThread
    private void feed1() {
        int index;
        while ((index = threadNum.getAndDecrement()) > 0) {
            Tester t = new Tester(index);
            f1.add(index);
            thdf.chainSubmit(t);
        }
    }

    @AsyncThread
    private void feed2() {
        int index;
        while ((index = threadNum.getAndDecrement()) > 0) {
            Tester t = new Tester(index);
            f2.add(index);
            thdf.chainSubmit(t);
        }
    }

    @Test
    public void test() throws InterruptedException {
        feed1();
        feed2();
        latch.await(2, TimeUnit.MINUTES);
        int pos = -1;
        for (Integer i : f1) {
            int p = res.indexOf(i);
            Assert.assertTrue(p > pos);
            pos = p;
        }
        pos = -1;
        for (Integer i : f2) {
            int p = res.indexOf(i);
            Assert.assertTrue(p > pos);
            pos = p;
        }
    }
}
