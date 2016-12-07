package org.zstack.test.compute.host;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostVO;
import org.zstack.header.simulator.SimulatorHostVO;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.test.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class TestHostDeleteExtensionPoint {
    CLogger logger = Utils.getLogger(TestChangeHostState.class);
    Api api;
    ComponentLoader loader;
    DatabaseFacade dbf;
    DeletHostExtension ext;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        BeanConstructor con = new WebBeanConstructor();
        /* This loads spring application context */
        loader = con.
                addXml("PortalForUnitTest.xml")
                .addXml("ClusterManager.xml")
                .addXml("ZoneManager.xml")
                .addXml("HostManager.xml")
                .addXml("Simulator.xml")
                .addXml("DeletHostExtension.xml")
                .addXml("HostAllocatorManager.xml")
                .addXml("AccountManager.xml").build();
        dbf = loader.getComponent(DatabaseFacade.class);
        ext = loader.getComponent(DeletHostExtension.class);
        api = new Api();
        api.startServer();
    }

    @Test
    public void test() throws ApiSenderException {
        try {
            ZoneInventory zone = api.createZones(1).get(0);
            ClusterInventory cluster = api.createClusters(1, zone.getUuid()).get(0);
            HostInventory host = api.createHost(1, cluster.getUuid()).get(0);
            ext.setPreventDelete(true);
            api.maintainHost(host.getUuid());
            try {
                api.deleteHost(host.getUuid());
            } catch (ApiSenderException e) {
            }
            HostVO vo = dbf.findByUuid(host.getUuid(), HostVO.class);
            Assert.assertNotNull(vo);

            ext.setPreventDelete(false);
            ext.setExpectedHostUuid(host.getUuid());
            api.deleteHost(host.getUuid());
            vo = dbf.findByUuid(host.getUuid(), HostVO.class);
            Assert.assertEquals(null, vo);
            SimulatorHostVO svo = dbf.findByUuid(host.getUuid(), SimulatorHostVO.class);
            Assert.assertEquals(null, svo);
            Assert.assertTrue(ext.isBeforeCalled());
            Assert.assertTrue(ext.isAfterCalled());
        } finally {
            api.stopServer();
        }
    }
}
