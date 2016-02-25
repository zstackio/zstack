package org.zstack.test.compute.vm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.identity.*;
import org.zstack.header.query.QueryOp;
import org.zstack.header.vm.APIQueryVmInstanceMsg;
import org.zstack.header.vm.APIQueryVmInstanceReply;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.identity.IdentityCreator;
import org.zstack.test.search.QueryTestValidator;

/**
 * 
 * @author Frank
 * 
 * @condition
 * 
 * 1. deploy a vm using non-admin account
 * 2. query vm on each queryable field of VmInstanceInventory using non-admin account
 * 
 * @test
 * the vm can be correctly found
 */
public class TestQueryVm {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/vm/TestQueryVm.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }
    
    @Test
    public void test() throws ApiSenderException {
        SessionInventory session = api.loginByAccount("test", "password");
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        QueryTestValidator.validateEQ(new APIQueryVmInstanceMsg(), api, APIQueryVmInstanceReply.class, vm, session);
        QueryTestValidator.validateRandomEQConjunction(new APIQueryVmInstanceMsg(), api, APIQueryVmInstanceReply.class, vm, session, 3);

        IdentityCreator c = new IdentityCreator(api);
        AccountInventory test = c.useAccount("test");

        APIQueryAccountResourceRefMsg qmsg = new APIQueryAccountResourceRefMsg();
        qmsg.addQueryCondition("accountUuid", QueryOp.EQ, test.getUuid());
        qmsg.addQueryCondition("resourceType", QueryOp.EQ, VmInstanceVO.class.getSimpleName());
        APIQueryAccountResourceRefReply r = api.query(qmsg, APIQueryAccountResourceRefReply.class);
        Assert.assertEquals(1, r.getInventories().size());
        AccountResourceRefInventory inv = r.getInventories().get(0);
        Assert.assertEquals(vm.getUuid(), inv.getResourceUuid());
    }
}
