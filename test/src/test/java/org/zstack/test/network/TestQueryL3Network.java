package org.zstack.test.network;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.network.l3.APIQueryL3NetworkMsg;
import org.zstack.header.network.l3.APIQueryL3NetworkReply;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.query.QueryCondition;
import org.zstack.header.query.QueryOp;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.search.QueryTestValidator;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class TestQueryL3Network {
    CLogger logger = Utils.getLogger(TestQueryL3Network.class);
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
        SessionInventory session = api.loginByAccount("TestAccount", "password");
        L3NetworkInventory l3inv = deployer.l3Networks.get("TestL3Network2");
        QueryTestValidator.validateEQ(new APIQueryL3NetworkMsg(), api, APIQueryL3NetworkReply.class, l3inv, session);
        QueryTestValidator.validateRandomEQConjunction(new APIQueryL3NetworkMsg(), api, APIQueryL3NetworkReply.class, l3inv, session, 2);

        APIQueryL3NetworkMsg msg = new APIQueryL3NetworkMsg();
        QueryCondition qc = new QueryCondition();
        qc.setName("dns");
        qc.setOp(QueryOp.EQ.toString());
        qc.setValue("9.9.9.9");
        msg.getConditions().add(qc);
        APIQueryL3NetworkReply reply = api.query(msg, APIQueryL3NetworkReply.class);
        Assert.assertEquals(1, reply.getInventories().size());
    }

}
