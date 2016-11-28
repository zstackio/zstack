package org.zstack.test.compute.host;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.host.APIQueryHostMsg;
import org.zstack.header.host.APIQueryHostReply;
import org.zstack.header.host.HostInventory;
import org.zstack.header.query.QueryCondition;
import org.zstack.header.query.QueryOp;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.search.QueryTestValidator;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;

import static org.zstack.utils.CollectionDSL.list;

public class TestQueryHost {
    CLogger logger = Utils.getLogger(TestQueryHost.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/host/TestQueryHost.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws InterruptedException, ApiSenderException {
        HostInventory inv = deployer.hosts.get("TestHost3");
        QueryTestValidator.validateEQ(new APIQueryHostMsg(), api, APIQueryHostReply.class, inv);
        QueryTestValidator.validateRandomEQConjunction(new APIQueryHostMsg(), api, APIQueryHostReply.class, inv, 3);

        APIQueryHostMsg msg = new APIQueryHostMsg();
        msg.setConditions(new ArrayList<QueryCondition>());
        APIQueryHostReply reply = api.query(msg, APIQueryHostReply.class);
        Assert.assertEquals(5, reply.getInventories().size());

        msg = new APIQueryHostMsg();
        // this case should not cause error
        msg.addQueryCondition("uuid", QueryOp.IN, ",,,,,");
        api.query(msg, APIQueryHostReply.class);

        boolean s = false;
        msg = new APIQueryHostMsg();
        msg.setFields(list("totalCpuCapacity"));
        msg.setConditions(new ArrayList<QueryCondition>());
        try {
            api.query(msg, APIQueryHostReply.class);
        } catch (ApiSenderException e) {
            s = true;
        }
        Assert.assertTrue(s);
    }
}
