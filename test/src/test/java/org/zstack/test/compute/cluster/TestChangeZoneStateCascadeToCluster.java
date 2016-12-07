package org.zstack.test.compute.cluster;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.cluster.ClusterState;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.header.zone.ZoneStateEvent;
import org.zstack.test.*;

public class TestChangeZoneStateCascadeToCluster {
    Api api;
    ComponentLoader loader;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        BeanConstructor con = new WebBeanConstructor();
        /* This loads spring application context */
        loader = con.addXml("PortalForUnitTest.xml").addXml("ClusterManager.xml").addXml("ZoneManager.xml").addXml("AccountManager.xml").build();
        dbf = loader.getComponent(DatabaseFacade.class);
        api = new Api();
        api.startServer();
    }

    @Test
    public void test() throws ApiSenderException {
        try {
            ZoneInventory zone = api.createZones(1).get(0);
            ClusterInventory cluster = api.createClusters(1, zone.getUuid()).get(0);
            api.changeZoneState(zone.getUuid(), ZoneStateEvent.disable);
            ClusterVO vo = dbf.findByUuid(cluster.getUuid(), ClusterVO.class);
            Assert.assertEquals(ClusterState.Disabled, vo.getState());
            api.changeZoneState(zone.getUuid(), ZoneStateEvent.enable);
            vo = dbf.findByUuid(cluster.getUuid(), ClusterVO.class);
            Assert.assertEquals(ClusterState.Enabled, vo.getState());
        } finally {
            api.stopServer();
        }
    }

}
