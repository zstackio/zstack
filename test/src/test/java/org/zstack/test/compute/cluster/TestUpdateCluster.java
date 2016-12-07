package org.zstack.test.compute.cluster;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.test.*;

public class TestUpdateCluster {
    Api api;
    ComponentLoader loader;
    DatabaseFacade dbf;

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

    @Test
    public void test() throws InterruptedException, ApiSenderException {
        ZoneInventory zone = api.createZones(1).get(0);
        ClusterInventory cluster = api.createClusters(1, zone.getUuid()).get(0);
        cluster.setName("1");
        cluster.setDescription("xxx");
        cluster = api.updateCluster(cluster);
        Assert.assertEquals("1", cluster.getName());
        Assert.assertEquals("xxx", cluster.getDescription());
    }

}
