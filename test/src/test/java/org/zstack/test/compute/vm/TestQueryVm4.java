package org.zstack.test.compute.vm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.host.HostVO;
import org.zstack.header.query.QueryOp;
import org.zstack.header.vm.APIQueryVmInstanceMsg;
import org.zstack.header.vm.APIQueryVmInstanceReply;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;

import java.util.List;

/**
 * @author Frank
 * @condition 1. deploy a vm using non-admin account
 * 2. set a user tag on host
 * @test query vm using user tag on host
 */
public class TestQueryVm4 {
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
        api.createUserTag(vm.getHostUuid(), "host", HostVO.class);

        HostVO host = dbf.findByUuid(vm.getHostUuid(), HostVO.class);

        APIQueryVmInstanceMsg msg = new APIQueryVmInstanceMsg();
        msg.addQueryCondition("host.__userTag__", QueryOp.EQ, "host");
        msg.addQueryCondition("host.__systemTag__", QueryOp.EQ, "os::release::simulator");
        List<VmInstanceInventory> vms = api.query(msg, APIQueryVmInstanceReply.class).getInventories();
        Assert.assertEquals(1, vms.size());

        VmInstanceInventory vm1 = vms.get(0);
        Assert.assertEquals(vm.getUuid(), vm1.getUuid());

        msg = new APIQueryVmInstanceMsg();
        msg.addQueryCondition("host.__userTag__", QueryOp.EQ, "host");
        msg.addQueryCondition("host.uuid", QueryOp.EQ, host.getUuid());
        vms = api.query(msg, APIQueryVmInstanceReply.class).getInventories();
        Assert.assertEquals(1, vms.size());
    }
}
