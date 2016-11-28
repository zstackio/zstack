package org.zstack.test.network;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.network.service.vip.VipInventory;
import org.zstack.network.service.vip.VipState;
import org.zstack.network.service.vip.VipStateEvent;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;

public class TestChangeVipState {
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
        L3NetworkInventory l3nw = deployer.l3Networks.get("TestL3Network1");
        VipInventory ip = api.acquireIp(l3nw.getUuid());
        Assert.assertEquals(VipState.Enabled.toString(), ip.getState());
        ip = api.changeVipSate(ip.getUuid(), VipStateEvent.disable);
        Assert.assertEquals(VipState.Disabled.toString(), ip.getState());
        ip = api.changeVipSate(ip.getUuid(), VipStateEvent.enable);
        Assert.assertEquals(VipState.Enabled.toString(), ip.getState());
    }
}
