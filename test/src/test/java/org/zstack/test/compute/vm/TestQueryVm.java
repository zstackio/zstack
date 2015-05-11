package org.zstack.test.compute.vm;

import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.vm.APIQueryVmInstanceMsg;
import org.zstack.header.vm.APIQueryVmInstanceReply;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;
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
        SessionInventory session = api.loginByAccount("TestAccount", "password");
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        QueryTestValidator.validateEQ(new APIQueryVmInstanceMsg(), api, APIQueryVmInstanceReply.class, vm, session);
        QueryTestValidator.validateRandomEQConjunction(new APIQueryVmInstanceMsg(), api, APIQueryVmInstanceReply.class, vm, session, 3);
    }
}
