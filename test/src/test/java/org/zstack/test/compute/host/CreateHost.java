package org.zstack.test.compute.host;

import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.test.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/* called by other test cases */
public class CreateHost {
    CLogger logger = Utils.getLogger(CreateHost.class);
    Api api;
    ComponentLoader loader;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        BeanConstructor con = new WebBeanConstructor();
        /* This loads spring application context */
        loader = con.addXml("PortalForUnitTest.xml")
                .addXml("ClusterManager.xml")
                .addXml("ZoneManager.xml")
                .addXml("HostManager.xml")
                .addXml("Simulator.xml")
                .addXml("HostAllocatorManager.xml")
                .addXml("AccountManager.xml").build();
        dbf = loader.getComponent(DatabaseFacade.class);
        api = new Api();
        api.startServer();
    }

    @Test
    public void test() throws ApiSenderException {
        try {
            int hostNum = Integer.valueOf(System.getProperty("host.num"));
            ZoneInventory zone = api.createZones(1).get(0);
            ClusterInventory cluster = api.createClusters(1, zone.getUuid()).get(0);
            api.createHost(hostNum, cluster.getUuid());
        } finally {
            api.stopServer();
        }
    }

}
