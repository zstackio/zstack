package org.zstack.test.compute.vm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.allocator.HostCapacityVO;
import org.zstack.header.host.HostInventory;
import org.zstack.header.vm.GetVmMigrationTargetHostMsg;
import org.zstack.header.vm.GetVmMigrationTargetHostReply;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;

import java.util.List;

/*
 * get migration target by using api
 */
public class TestMigrateVm4 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/vm/TestCreateVm.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }
    
    @Test
    public void test() throws ApiSenderException {
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        List<HostInventory> hosts = api.getMigrationTargetHost(vm.getUuid());
        for (HostInventory targetHost : hosts) {
            HostCapacityVO hcvo = dbf.findByUuid(targetHost.getUuid(), HostCapacityVO.class);
            Assert.assertEquals(0, hcvo.getUsedCpu());
            Assert.assertEquals(0, hcvo.getUsedMemory());
        }
    }
}
