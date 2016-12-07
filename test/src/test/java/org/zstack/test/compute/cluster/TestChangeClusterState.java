package org.zstack.test.compute.cluster;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.thread.AsyncThread;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.cluster.ClusterState;
import org.zstack.header.cluster.ClusterStateEvent;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.test.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

public class TestChangeClusterState {
    CLogger logger = Utils.getLogger(TestChangeClusterState.class);
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
        loader = con.addXml("PortalForUnitTest.xml").addXml("ZoneManager.xml").addXml("ClusterManager.xml").addXml("AccountManager.xml").build();
        dbf = loader.getComponent(DatabaseFacade.class);
        api = new Api();
        api.startServer();
    }

    @AsyncThread
    void enableCluster(ClusterInventory cluster) throws ApiSenderException, InterruptedException, BrokenBarrierException {
        try {
            barrier.await();
            for (int i = 0; i < testNum; i++) {
                ClusterInventory c = api.changeClusterState(cluster.getUuid(), ClusterStateEvent.enable);
                if (!c.getState().equals(ClusterState.Enabled.toString())) {
                    this.isEnableSuccess = false;
                    return;
                }
            }
            this.isEnableSuccess = true;
        } finally {
            latch.countDown();
        }
    }

    @AsyncThread
    void disableCluster(ClusterInventory cluster) throws ApiSenderException, InterruptedException, BrokenBarrierException {
        try {
            barrier.await();
            for (int i = 0; i < testNum; i++) {
                ClusterInventory c = api.changeClusterState(cluster.getUuid(), ClusterStateEvent.disable);
                if (!c.getState().equals(ClusterState.Disabled.toString())) {
                    this.isDisableSuccess = false;
                    return;
                }
            }
            this.isDisableSuccess = true;
        } finally {
            latch.countDown();
        }
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException, BrokenBarrierException {
        try {
            ZoneInventory zone = api.createZones(1).get(0);
            ClusterInventory cluster = api.createClusters(1, zone.getUuid()).get(0);
            this.enableCluster(cluster);
            this.disableCluster(cluster);
            barrier.await();
            latch.await(60, TimeUnit.SECONDS);
            Assert.assertTrue(isDisableSuccess);
            Assert.assertTrue(isEnableSuccess);
        } finally {
            api.stopServer();
        }
    }

}
