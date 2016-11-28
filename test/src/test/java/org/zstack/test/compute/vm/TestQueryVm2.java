package org.zstack.test.compute.vm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.query.QueryCondition;
import org.zstack.header.query.QueryOp;
import org.zstack.header.vm.APIQueryVmInstanceMsg;
import org.zstack.header.vm.APIQueryVmInstanceReply;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;

/**
 * @author Frank
 * @condition 1. deploy a vm using non-admin account: TestAccount
 * 2. create another non-admin account: TestAccount1
 * 2. query vm using TestAccount1
 * @test the vm can be not be seen by TestAccount1
 */
public class TestQueryVm2 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/vm/TestQueryVm2.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException {
        SessionInventory session = api.loginByAccount("TestAccount1", "password");
        APIQueryVmInstanceMsg msg = new APIQueryVmInstanceMsg();
        QueryCondition c = new QueryCondition();
        c.setName("name");
        c.setOp(QueryOp.EQ.toString());
        c.setValue("TestVm");
        msg.getConditions().add(c);
        APIQueryVmInstanceReply reply = api.query(msg, APIQueryVmInstanceReply.class, session);
        Assert.assertEquals(0, reply.getInventories().size());
    }
}
