package org.zstack.test.network;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.network.service.vip.VipInventory;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;

public class TestAcquireRequiredVip1 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/network/TestAcquireIp.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException {
        String rip = "192.168.1.5";
        L3NetworkInventory l3nw = deployer.l3Networks.get("TestL3Network2");
        api.addIpRange(l3nw.getUuid(), "192.168.1.2", "192.168.1.254", "192.168.1.1", "255.255.255.0");
        VipInventory ip = api.acquireIp(l3nw.getUuid(), rip);
        Assert.assertEquals(ip.getIp(), rip);
    }

}
