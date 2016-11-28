package org.zstack.test.network;

import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.network.l2.L2NetworkConstant;
import org.zstack.header.network.l2.L2VlanNetworkInventory;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;

/**
 * 1. attach another vlan l2 to cluster that already has a vlan l2 having the same physical interface, vlan attached
 * <p>
 * confirm attach fail
 */
public class TestAttachL2NetworkToCluster2 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/network/TestAttachL2NetworkToCluster2.xml");
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

        L2VlanNetworkInventory vlan = new L2VlanNetworkInventory();
        vlan.setName("vlan2");
        vlan.setType(L2NetworkConstant.L2_VLAN_NETWORK_TYPE);
        vlan.setZoneUuid(zone.getUuid());
        vlan.setPhysicalInterface("eth0");
        vlan.setVlan(10);
        L2VlanNetworkInventory l2 = api.createL2VlanNetworkByFullConfig(vlan);
        api.attachL2NetworkToCluster(l2.getUuid(), cluster.getUuid());
    }
}
