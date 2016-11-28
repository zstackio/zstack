package org.zstack.test.network;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.network.l3.APIQueryIpRangeMsg;
import org.zstack.header.network.l3.APIQueryIpRangeReply;
import org.zstack.header.network.l3.IpRangeInventory;
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

public class TestQueryIpRange {
    CLogger logger = Utils.getLogger(TestQueryIpRange.class);
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
        IpRangeInventory iprange = l3inv.getIpRanges().get(0);
        QueryTestValidator.validateEQ(new APIQueryIpRangeMsg(), api, APIQueryIpRangeReply.class, iprange, session);
        QueryTestValidator.validateRandomEQConjunction(new APIQueryIpRangeMsg(), api, APIQueryIpRangeReply.class, iprange, session, 2);

        l3inv = deployer.l3Networks.get("TestL3Network3");
        APIQueryIpRangeMsg msg = new APIQueryIpRangeMsg();
        iprange = l3inv.getIpRanges().get(0);
        QueryCondition qc = new QueryCondition();
        qc.setName("uuid");
        qc.setOp(QueryOp.EQ.toString());
        qc.setValue(iprange.getUuid());
        msg.getConditions().add(qc);
        APIQueryIpRangeReply reply = api.query(msg, APIQueryIpRangeReply.class, session);
        Assert.assertEquals(0, reply.getInventories().size());
    }

}
