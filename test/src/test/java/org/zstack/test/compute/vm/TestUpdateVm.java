package org.zstack.test.compute.vm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.compute.vm.VmSystemTags;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;

import java.util.concurrent.TimeUnit;

public class TestUpdateVm {
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
    public void test() throws ApiSenderException, InterruptedException {
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        vm.setName("1");
        vm.setDescription("xxx");
        vm.setState(VmInstanceState.Stopped.toString());
        vm = api.updateVm(vm);

        Assert.assertEquals("1", vm.getName());
        Assert.assertEquals("xxx", vm.getDescription());
        Assert.assertEquals(VmInstanceState.Stopped.toString(), vm.getState());

        vm = api.updateCpuMemory(vm.getUuid(), null, Long.valueOf(4294967296l));
        Assert.assertEquals("1", VmSystemTags.PENDING_CAPACITY_CHANGE.getTokenByResourceUuid(vm.getUuid(), VmSystemTags.PENDING_CAPACITY_CHNAGE_CPU_NUM_TOKEN));
        Assert.assertEquals("3000", VmSystemTags.PENDING_CAPACITY_CHANGE.getTokenByResourceUuid(vm.getUuid(), VmSystemTags.PENDING_CAPACITY_CHNAGE_CPU_SPEED_TOKEN));
        Assert.assertEquals("4294967296", VmSystemTags.PENDING_CAPACITY_CHANGE.getTokenByResourceUuid(vm.getUuid(), VmSystemTags.PENDING_CAPACITY_CHNAGE_MEMORY_TOKEN));

        vm = api.updateCpuMemory(vm.getUuid(), 2, null);
        Assert.assertEquals("2", VmSystemTags.PENDING_CAPACITY_CHANGE.getTokenByResourceUuid(vm.getUuid(), VmSystemTags.PENDING_CAPACITY_CHNAGE_CPU_NUM_TOKEN));
        // please note the expectedMemory value!
        Assert.assertEquals("3221225472", VmSystemTags.PENDING_CAPACITY_CHANGE.getTokenByResourceUuid(vm.getUuid(), VmSystemTags.PENDING_CAPACITY_CHNAGE_MEMORY_TOKEN));

        vm = api.updateCpuMemory(vm.getUuid(), 2, Long.valueOf(4294967296l));
        Assert.assertEquals("pendingCapacityChange::cpuNum::2::cpuSpeed::3000::memory::4294967296", VmSystemTags.PENDING_CAPACITY_CHANGE.getTag(vm.getUuid()).toString());

        vm = api.stopVmInstance(vm.getUuid());
        TimeUnit.SECONDS.sleep(1);

        vm = api.startVmInstance(vm.getUuid());
        TimeUnit.SECONDS.sleep(1);

        Assert.assertEquals(Long.valueOf(4294967296l), vm.getMemorySize());
        Assert.assertEquals(Integer.valueOf(2), vm.getCpuNum());
        Assert.assertEquals(VmInstanceState.Running.toString(), vm.getState());
    }

}
