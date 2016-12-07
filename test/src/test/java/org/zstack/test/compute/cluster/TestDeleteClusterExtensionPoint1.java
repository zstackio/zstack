package org.zstack.test.compute.cluster;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.test.*;

public class TestDeleteClusterExtensionPoint1 {
    Api api;
    ComponentLoader loader;
    DatabaseFacade dbf;
    ClusterDeleteExtension ext;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        BeanConstructor con = new WebBeanConstructor();
        /* This loads spring application context */
        loader = con.addXml("PortalForUnitTest.xml").addXml("ClusterManager.xml").addXml("ZoneManager.xml").addXml("ClusterForUnitTest.xml").addXml("AccountManager.xml").build();
        dbf = loader.getComponent(DatabaseFacade.class);
        ext = loader.getComponent(ClusterDeleteExtension.class);
        api = new Api();
        api.startServer();
    }

    @Test
    public void test() throws ApiSenderException {
        try {
            ZoneInventory zone = api.createZones(1).get(0);
            ClusterInventory cluster = api.createClusters(1, zone.getUuid()).get(0);
            ext.setPreventDelete(true);
            try {
                api.deleteZone(zone.getUuid());
            } catch (Exception e) {
            }
            ClusterVO vo = dbf.findByUuid(cluster.getUuid(), ClusterVO.class);
            Assert.assertNotNull(vo);
            ext.setPreventDelete(false);
            api.deleteZone(zone.getUuid());
            Assert.assertTrue(ext.isBeforeCalled());
            Assert.assertTrue(ext.isAfterCalled());
            vo = dbf.findByUuid(cluster.getUuid(), ClusterVO.class);
            Assert.assertNull(vo);
        } finally {
            api.stopServer();
        }
    }


}
