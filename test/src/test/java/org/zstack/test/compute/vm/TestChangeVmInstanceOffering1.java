package org.zstack.test.compute.vm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.allocator.APIGetCpuMemoryCapacityReply;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;

/**
 * 1. change instance offering of the vm
 * 2. stop/start the vm
 * <p>
 * confirm the cpu/memory capacity of the system changed
 */
public class TestChangeVmInstanceOffering1 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/vm/TestChangeVmInstanceOffering.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException {
        VmInstanceInventory vm = deployer.vms.get("TestVm");

        APIGetCpuMemoryCapacityReply cap1 = api.retrieveHostCapacityByAll();

        InstanceOfferingInventory ioinv2 = deployer.instanceOfferings.get("TestInstanceOffering1");
        vm = api.changeInstanceOffering(vm.getUuid(), ioinv2.getUuid());

        api.stopVmInstance(vm.getUuid());
        api.startVmInstance(vm.getUuid());

        APIGetCpuMemoryCapacityReply cap2 = api.retrieveHostCapacityByAll();
        Assert.assertTrue(String.format("CPU: cap1: %s != cap2: %s", cap1.getAvailableCpu(), cap2.getAvailableCpu()), cap1.getAvailableCpu() != cap2.getAvailableCpu());
        Assert.assertTrue(String.format("MEM: cap1: %s != cap2: %s", cap1.getAvailableMemory(), cap2.getAvailableMemory()), cap1.getAvailableMemory() != cap2.getAvailableMemory());
    }
}
