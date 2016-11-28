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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;


public class TestThreadFacadeSyncReturnValue {
    private static final CLogger logger = Utils.getLogger(TestThreadFacadeSyncReturnValue.class);
    ComponentLoader loader;
    ThreadFacade thdf;
    int threadNum = 5;

    class Tester implements SyncTask<Integer> {
        int index;

        Tester(int index) {
            this.index = index;
        }

        @Override
        public Integer call() throws Exception {
            return index;
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

    @Test
    public void test() throws InterruptedException, ExecutionException {
        Future<Integer> ret = null;
        for (int i = 0; i < threadNum; i++) {
            Tester worker = new Tester(i);
            Future<Integer> f = thdf.syncSubmit(worker);
            if (i == this.threadNum - 1) {
                ret = f;
            }
        }
        Assert.assertEquals(this.threadNum - 1, ret.get().intValue());
    }
}
