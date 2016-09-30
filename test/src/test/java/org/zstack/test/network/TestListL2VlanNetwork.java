package org.zstack.test.network;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.network.l2.L2VlanNetworkInventory;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;

public class TestListL2VlanNetwork {
    CLogger logger = Utils.getLogger(TestListL2VlanNetwork.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/network/TestListL2VlanNetwork.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException {
        List<L2VlanNetworkInventory> invs = api.listL2VlanNetworks(null);
        Assert.assertEquals(3, invs.size());
        int vlan = 1 + 2 + 3;
        int target = 0;
        for (L2VlanNetworkInventory inv : invs) {
            target += inv.getVlan();
        }
        Assert.assertEquals(vlan, target);
    }
}
