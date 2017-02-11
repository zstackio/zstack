package org.zstack.test.core.thread;

import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBusImpl2;
import org.zstack.core.cloudbus.EventFacadeImpl;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.debug.DebugSignal;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

// this is for manual testing, don't add it to unit test suite
public class TestChainTask5 {
    CLogger logger = Utils.getLogger(TestChainTask5.class);
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
            return String.format("test-%s", index);
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
        }
    }

    ComponentLoader loader;
    Deployer deployer;
    DatabaseFacade dbf;
    ThreadFacade thdf;
    EventFacadeImpl evtf;
    CloudBusImpl2 bus;
    Api api;

    @Before
    public void setUp() throws Exception {
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/OnlyOneZone.xml", con);
        deployer.load();
        loader = deployer.getComponentLoader();
        dbf = loader.getComponent(DatabaseFacade.class);
        thdf = loader.getComponent(ThreadFacade.class);
        bus = loader.getComponent(CloudBusImpl2.class);
        deployer.build();
        api = deployer.getApi();
    }

    @Test
    public void test() throws InterruptedException, ApiSenderException {
        for (int i = 0; i < threadNum; i++) {
            Tester t = new Tester(i);
            thdf.chainSubmit(t);
        }

        TimeUnit.SECONDS.sleep(5);
        sendSignal();
        latch.await(2, TimeUnit.MINUTES);
    }

    private void sendSignal() throws ApiSenderException {
        api.debugSignal(DebugSignal.DumpTaskQueue);
    }
}
