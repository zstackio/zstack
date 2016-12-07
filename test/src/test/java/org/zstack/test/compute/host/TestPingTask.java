package org.zstack.test.compute.host;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.compute.host.HostGlobalConfig;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.config.GlobalConfigFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostStatus;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.simulator.SimulatorController;
import org.zstack.test.*;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class TestPingTask {
    Api api;
    ComponentLoader loader;
    DatabaseFacade dbf;
    GlobalConfigFacade gcf;
    SimulatorController sctrl;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        BeanConstructor con = new WebBeanConstructor();
        /* This loads spring application context */
        loader = con.addXml("PortalForUnitTest.xml").addXml("ClusterManager.xml")
                .addXml("ZoneManager.xml").addXml("HostManager.xml")
                .addXml("Simulator.xml").addXml("AccountManager.xml")
                .addXml("HostAllocatorManager.xml").build();
        dbf = loader.getComponent(DatabaseFacade.class);
        gcf = loader.getComponent(GlobalConfigFacade.class);
        sctrl = loader.getComponent(SimulatorController.class);
        HostGlobalConfig.PING_HOST_INTERVAL.updateValue(1);
        api = new Api();
        api.startServer();
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        ZoneInventory zone = api.createZones(1).get(0);
        ClusterInventory cluster = api.createClusters(1, zone.getUuid()).get(0);
        HostInventory host = api.createHost(1, cluster.getUuid()).get(0);
        HostGlobalConfig.AUTO_RECONNECT_ON_ERROR.updateValue(false);
        TimeUnit.SECONDS.sleep(1);
        sctrl.setSimulatorHostConnectionState(host.getUuid(), true);
        UnitTestUtils.sleepRetry(new Callable() {
            @Override
            public Object call() throws Exception {
                HostInventory host = api.listHosts(null).get(0);
                Assert.assertEquals(HostStatus.Disconnected.toString(), host.getStatus());
                return null;
            }
        }, 10);
        TimeUnit.SECONDS.sleep(5);

        HostGlobalConfig.AUTO_RECONNECT_ON_ERROR.updateValue(true);
        TimeUnit.SECONDS.sleep(1);
        sctrl.setSimulatorHostConnectionState(host.getUuid(), true);
        UnitTestUtils.sleepRetry(new Callable() {
            @Override
            public Object call() throws Exception {
                HostInventory host = api.listHosts(null).get(0);
                Assert.assertEquals(HostStatus.Connected.toString(), host.getStatus());
                return null;
            }
        }, 10);

        sctrl.setSimulatorHostConnectionState(host.getUuid(), false);
        UnitTestUtils.sleepRetry(new Callable() {
            @Override
            public Object call() throws Exception {
                HostInventory host = api.listHosts(null).get(0);
                Assert.assertEquals(HostStatus.Connected.toString(), host.getStatus());
                return null;
            }
        }, 10);
    }
}
