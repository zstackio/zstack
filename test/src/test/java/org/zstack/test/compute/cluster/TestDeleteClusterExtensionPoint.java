package org.zstack.test.compute.cluster;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.cluster.APIDeleteClusterEvent;
import org.zstack.header.cluster.APIDeleteClusterMsg;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.test.*;

public class TestDeleteClusterExtensionPoint {
    Api api;
    ComponentLoader loader;
    DatabaseFacade dbf;
    ClusterDeleteExtension ext;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        BeanConstructor con = new WebBeanConstructor();
        /* This loads spring application context */
        loader = con.addXml("PortalForUnitTest.xml").
                addXml("ClusterManager.xml").
                addXml("ZoneManager.xml").
                addXml("ClusterForUnitTest.xml").
                addXml("AccountManager.xml").
                build();
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
            APIDeleteClusterMsg msg = new APIDeleteClusterMsg(cluster.getUuid());
            msg.setSession(api.getAdminSession());
            ext.setPreventDelete(true);
            api.getApiSender().send(msg, APIDeleteClusterEvent.class, false);
            ClusterVO vo = dbf.findByUuid(cluster.getUuid(), ClusterVO.class);
            Assert.assertNotNull(vo);
            ext.setPreventDelete(false);
            api.getApiSender().send(msg, APIDeleteClusterEvent.class);
            Assert.assertTrue(ext.isBeforeCalled());
            Assert.assertTrue(ext.isAfterCalled());
            vo = dbf.findByUuid(cluster.getUuid(), ClusterVO.class);
            Assert.assertEquals(null, vo);
        } finally {
            api.stopServer();
        }
    }


}
