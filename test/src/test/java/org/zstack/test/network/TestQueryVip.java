package org.zstack.test.network;

import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.network.service.vip.APIQueryVipMsg;
import org.zstack.network.service.vip.APIQueryVipReply;
import org.zstack.network.service.vip.VipInventory;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.search.QueryTestValidator;

public class TestQueryVip {
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
        QueryTestValidator.validateEQ(new APIQueryVipMsg(), api, APIQueryVipReply.class, ip, api.getAdminSession());
        QueryTestValidator.validateRandomEQConjunction(new APIQueryVipMsg(), api, APIQueryVipReply.class, ip, api.getAdminSession(), 3);
    }

}
