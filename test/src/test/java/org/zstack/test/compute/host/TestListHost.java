package org.zstack.test.compute.host;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.host.HostInventory;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.test.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;

@Deprecated
public class TestListHost {
    CLogger logger = Utils.getLogger(TestListHost.class);
    Api api;
    ComponentLoader loader;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        BeanConstructor con = new WebBeanConstructor();
        /* This loads spring application context */
        loader = con.addXml("PortalForUnitTest.xml").addXml("ClusterManager.xml")
                .addXml("ZoneManager.xml").addXml("HostManager.xml").addXml("Simulator.xml").addXml("AccountManager.xml").build();
        dbf = loader.getComponent(DatabaseFacade.class);
        api = new Api();
        api.startServer();
    }

    @Test
    public void test() throws ApiSenderException {
        try {
            ZoneInventory zone = api.createZones(1).get(0);
            ClusterInventory cluster = api.createClusters(1, zone.getUuid()).get(0);
            api.createHost(10, cluster.getUuid());
            List<HostInventory> hosts = api.listHosts(null);
            Assert.assertEquals(10, hosts.size());
            List<String> uuids = new ArrayList<String>(5);
            for (int i = 0; i < 5; i++) {
                uuids.add(hosts.get(i).getUuid());
            }
            hosts = api.listHosts(uuids);
            for (int i = 0; i < 5; i++) {
                Assert.assertEquals(uuids.get(i), hosts.get(i).getUuid());
            }
        } finally {
            api.stopServer();
        }
    }
}
