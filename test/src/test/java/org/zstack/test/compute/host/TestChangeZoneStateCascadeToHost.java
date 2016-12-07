package org.zstack.test.compute.host;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostState;
import org.zstack.header.host.HostVO;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.header.zone.ZoneStateEvent;
import org.zstack.test.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;

public class TestChangeZoneStateCascadeToHost {
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
        loader = con.addXml("PortalForUnitTest.xml").addXml("ClusterManager.xml").addXml("ZoneManager.xml")
                .addXml("HostManager.xml").addXml("Simulator.xml").addXml("AccountManager.xml")
                .addXml("HostAllocatorManager.xml").build();
        dbf = loader.getComponent(DatabaseFacade.class);
        api = new Api();
        api.startServer();
    }

    @Test
    public void test() throws ApiSenderException {
        try {
            ZoneInventory zone = api.createZones(1).get(0);
            ClusterInventory cluster = api.createClusters(1, zone.getUuid()).get(0);
            HostInventory host = api.createHost(1, cluster.getUuid()).get(0);
            api.changeZoneState(zone.getUuid(), ZoneStateEvent.disable);
            HostVO vo = dbf.findByUuid(host.getUuid(), HostVO.class);
            Assert.assertEquals(HostState.Disabled, vo.getState());
        } finally {
            api.stopServer();
        }
    }

}
