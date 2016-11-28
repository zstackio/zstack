package org.zstack.test.network;

import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;

/**
 * 1. attach another l2 to cluster that already has a l2 having the same physical interface attached
 * <p>
 * confirm attach fail
 */
public class TestAttachL2NetworkToCluster1 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/network/TestAttachL2NetworkToCluster.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test(expected = ApiSenderException.class)
    public void test() throws ApiSenderException {
        ZoneInventory zone = deployer.zones.get("TestZone");
        ClusterInventory cluster = deployer.clusters.get("TestCluster");

        L2NetworkInventory l2 = api.createNoVlanL2Network(zone.getUuid(), "eth0");
        api.attachL2NetworkToCluster(l2.getUuid(), cluster.getUuid());
    }
}
