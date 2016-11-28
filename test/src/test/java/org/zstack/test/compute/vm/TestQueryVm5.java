package org.zstack.test.compute.vm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.query.QueryOp;
import org.zstack.header.vm.APIQueryVmInstanceMsg;
import org.zstack.header.vm.APIQueryVmInstanceReply;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;

import static org.zstack.utils.CollectionDSL.list;

/**
 * @author Frank
 * @condition 1. create a vm
 * 2. stop the vm
 * 3. query the hostUuid field
 * @test confirm the hostUuid returns null
 */
public class TestQueryVm5 {
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
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        api.stopVmInstance(vm.getUuid());
        APIQueryVmInstanceMsg msg = new APIQueryVmInstanceMsg();
        msg.setFields(list("hostUuid"));
        msg.addQueryCondition("uuid", QueryOp.EQ, vm.getUuid());
        APIQueryVmInstanceReply reply = api.query(msg, APIQueryVmInstanceReply.class);
        Assert.assertFalse(reply.getInventories().isEmpty());
        vm = reply.getInventories().get(0);
        Assert.assertNull(vm.getHostUuid());
    }
}
