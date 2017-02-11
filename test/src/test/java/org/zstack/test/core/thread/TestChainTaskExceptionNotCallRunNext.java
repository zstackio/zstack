package org.zstack.test.core.thread;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
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

public class TestChainTaskExceptionNotCallRunNext {
    CLogger logger = Utils.getLogger(TestChainTaskExceptionNotCallRunNext.class);
    ComponentLoader loader;
    ThreadFacade thdf;
    int threadNum = 100;
    List<Integer> res = new ArrayList<Integer>(threadNum);
    CountDownLatch latch = new CountDownLatch(threadNum);

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
            throw new RuntimeException("On purpose");
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
            Tester t = new Tester(i);
            thdf.chainSubmit(t);
        }

        latch.await(2, TimeUnit.MINUTES);
        int si = -1;
        for (Integer index : res) {
            Assert.assertTrue(index > si);
            si = index;
        }
    }
}
