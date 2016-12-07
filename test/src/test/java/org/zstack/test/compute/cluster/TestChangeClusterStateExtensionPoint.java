package org.zstack.test.compute.cluster;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.cluster.ClusterState;
import org.zstack.header.cluster.ClusterStateEvent;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.test.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class TestChangeClusterStateExtensionPoint {
    CLogger logger = Utils.getLogger(TestChangeClusterStateExtensionPoint.class);
    Api api;
    ComponentLoader loader;
    DatabaseFacade dbf;
    ClusteChangeStateExtension ext;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        BeanConstructor con = new WebBeanConstructor();
        /* This loads spring application context */
        loader = con.addXml("PortalForUnitTest.xml").addXml("ZoneManager.xml").addXml("ClusterManager.xml").addXml("ClusterForUnitTest.xml").addXml("AccountManager.xml").build();
        dbf = loader.getComponent(DatabaseFacade.class);
        ext = loader.getComponent(ClusteChangeStateExtension.class);
        api = new Api();
        api.startServer();
    }

    @Test
    public void test() throws ApiSenderException {
        try {
            ZoneInventory zone = api.createZones(1).get(0);
            ClusterInventory cluster = api.createClusters(1, zone.getUuid()).get(0);
            ext.setPreventChange(true);
            try {
                api.changeClusterState(cluster.getUuid(), ClusterStateEvent.disable);
            } catch (ApiSenderException e) {
            }
            ClusterVO vo = dbf.findByUuid(cluster.getUuid(), ClusterVO.class);
            Assert.assertEquals(ClusterState.Enabled, vo.getState());

            ext.setPreventChange(false);
            ext.setExpectedCurrent(ClusterState.Enabled);
            ext.setExpectedNext(ClusterState.Disabled);
            ext.setExpectedStateEvent(ClusterStateEvent.disable);
            api.changeClusterState(cluster.getUuid(), ClusterStateEvent.disable);
            vo = dbf.findByUuid(cluster.getUuid(), ClusterVO.class);
            Assert.assertEquals(ClusterState.Disabled, vo.getState());
            Assert.assertTrue(ext.isBeforeSuccess());
            Assert.assertTrue(ext.isAfterSuccess());
        } finally {
            api.stopServer();
        }
    }

}
