package org.zstack.test.core.thread;

import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.thread.SyncTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.test.BeanConstructor;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


public class TestThreadFacadeSyncReturnValueTimeout {
    private static final CLogger logger = Utils.getLogger(TestThreadFacadeSyncReturnValueTimeout.class);
    ComponentLoader loader;
    ThreadFacade thdf;
    int threadNum = 5;

    class Tester implements SyncTask<String> {
        int index;

        Tester(int index) {
            this.index = index;
        }

        @Override
        public String call() throws Exception {
            if (index == threadNum - 1) {
                Thread.sleep(5000);
            }
            return "I am back";
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
            return 2;
        }
    }

    @Before
    public void setUp() throws Exception {
        BeanConstructor con = new BeanConstructor();
        loader = con.build();
        thdf = loader.getComponent(ThreadFacade.class);
    }

    @Test(expected = TimeoutException.class)
    public void test() throws InterruptedException, ExecutionException, TimeoutException {
        Future<String> ret = null;
        for (int i = 0; i < this.threadNum; i++) {
            Tester worker = new Tester(i);
            Future<String> f = thdf.syncSubmit(worker);
            if (i == this.threadNum - 1) {
                ret = f;
            }
        }
        logger.info("Return value: " + ret.get(3, TimeUnit.SECONDS));
    }
}
