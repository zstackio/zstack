package org.zstack.test.compute.vm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.query.QueryOp;
import org.zstack.header.vm.APIQueryVmInstanceMsg;
import org.zstack.header.vm.APIQueryVmInstanceReply;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.search.QueryTestValidator;

import java.util.List;

/**
 * @author Frank
 * @condition test nested query
 * @test the vm can be correctly found
 */
public class TestQueryVm3 {
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

        vm = deployer.vms.get("TestVm");
        VmNicInventory nic = vm.getVmNics().get(0);
        APIQueryVmInstanceMsg imsg = new APIQueryVmInstanceMsg();
        imsg.addQueryCondition("vmNics.uuid", QueryOp.EQ, nic.getUuid());
        imsg.addQueryCondition("vmNics.l3NetworkUuid", QueryOp.NOT_EQ, Platform.getUuid());
        imsg.addQueryCondition("name", QueryOp.EQ, vm.getName());
        imsg.addQueryCondition("allVolumes.uuid", QueryOp.IN, vm.getRootVolumeUuid());
        List<VmInstanceInventory> vms = api.query(imsg, APIQueryVmInstanceReply.class).getInventories();
        Assert.assertEquals(1, vms.size());

        imsg = new APIQueryVmInstanceMsg();
        imsg.addQueryCondition("vmNics.uuid", QueryOp.EQ, nic.getUuid());
        imsg.addQueryCondition("vmNics.l3NetworkUuid", QueryOp.EQ, Platform.getUuid());
        imsg.addQueryCondition("name", QueryOp.EQ, vm.getName());
        imsg.addQueryCondition("allVolumes.uuid", QueryOp.IN, vm.getRootVolumeUuid());
        vms = api.query(imsg, APIQueryVmInstanceReply.class).getInventories();
        Assert.assertEquals(0, vms.size());
    }
}
