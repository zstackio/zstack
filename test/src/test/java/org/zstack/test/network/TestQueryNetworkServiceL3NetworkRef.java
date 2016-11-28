package org.zstack.test.network;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.network.service.APIQueryNetworkServiceL3NetworkRefMsg;
import org.zstack.header.network.service.APIQueryNetworkServiceL3NetworkRefReply;
import org.zstack.header.query.QueryCondition;
import org.zstack.header.query.QueryOp;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class TestQueryNetworkServiceL3NetworkRef {
    CLogger logger = Utils.getLogger(TestQueryNetworkServiceL3NetworkRef.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/network/TestQueryL3Network.xml");
        deployer.addSpringConfig("VirtualRouter.xml");
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        SessionInventory session = api.loginByAccount("TestAccount1", "password");
        APIQueryNetworkServiceL3NetworkRefMsg msg = new APIQueryNetworkServiceL3NetworkRefMsg();
        QueryCondition qc = new QueryCondition();
        qc.setName("networkServiceType");
        qc.setOp(QueryOp.IN.toString());
        qc.setValues("DNS", "DHCP");
        msg.getConditions().add(qc);
        APIQueryNetworkServiceL3NetworkRefReply reply = api.query(msg, APIQueryNetworkServiceL3NetworkRefReply.class, session);
        Assert.assertEquals(2, reply.getInventories().size());

        reply = api.query(msg, APIQueryNetworkServiceL3NetworkRefReply.class);
        Assert.assertEquals(2, reply.getInventories().size());
    }

}
