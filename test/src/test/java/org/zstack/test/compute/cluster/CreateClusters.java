package org.zstack.test.compute.cluster;

import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.test.*;

/* called by other test case to generate database records for cluster*/
public class CreateClusters {
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
    public void test() throws ApiSenderException {
        try {
            int clusterNum = Integer.valueOf(System.getProperty("cluster.num"));
            ZoneInventory zone = api.createZones(1).get(0);
            api.createClusters(clusterNum, zone.getUuid());
        } finally {
            api.stopServer();
        }
    }

}
