package org.zstack.test.compute.host;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.thread.AsyncThread;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostState;
import org.zstack.header.host.HostStateEvent;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.test.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

public class TestChangeHostState {
    CLogger logger = Utils.getLogger(TestChangeHostState.class);
    Api api;
    ComponentLoader loader;
    DatabaseFacade dbf;
    CountDownLatch latch = new CountDownLatch(2);
    boolean isEnableSuccess = false;
    boolean isDisableSuccess = true;
    int testNum = 20;
    CyclicBarrier barrier = new CyclicBarrier(3);

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        BeanConstructor con = new WebBeanConstructor();
        /* This loads spring application context */
        loader = con.addXml("PortalForUnitTest.xml")
                .addXml("ClusterManager.xml")
                .addXml("ZoneManager.xml")
                .addXml("HostManager.xml")
                .addXml("Simulator.xml")
                .addXml("HostAllocatorManager.xml")
                .addXml("AccountManager.xml").build();
        dbf = loader.getComponent(DatabaseFacade.class);
        api = new Api();
        api.startServer();
    }

    @AsyncThread
    void enableHost(HostInventory host) throws InterruptedException, BrokenBarrierException, ApiSenderException {
        barrier.await();
        try {
            for (int i = 0; i < testNum; i++) {
                HostInventory h = api.changeHostState(host.getUuid(), HostStateEvent.enable);
                if (!h.getState().equals(HostState.Enabled.toString())) {
                    return;
                }
            }
            this.isEnableSuccess = true;
        } finally {
            latch.countDown();
        }
    }

    @AsyncThread
    void disableHost(HostInventory host) throws InterruptedException, BrokenBarrierException, ApiSenderException {
        barrier.await();
        try {
            for (int i = 0; i < testNum; i++) {
                HostInventory h = api.changeHostState(host.getUuid(), HostStateEvent.disable);
                if (!h.getState().equals(HostState.Disabled.toString())) {
                    return;
                }
            }
            this.isDisableSuccess = true;
        } finally {
            latch.countDown();
        }
    }

    @Test
    public void test() throws InterruptedException, ApiSenderException, BrokenBarrierException {
        try {
            ZoneInventory zone = api.createZones(1).get(0);
            ClusterInventory cluster = api.createClusters(1, zone.getUuid()).get(0);
            HostInventory host = api.createHost(1, cluster.getUuid()).get(0);
            enableHost(host);
            disableHost(host);
            barrier.await();
            latch.await(60, TimeUnit.SECONDS);
            Assert.assertTrue(this.isEnableSuccess);
            Assert.assertTrue(this.isDisableSuccess);
        } finally {
            api.stopServer();
        }
    }
}
