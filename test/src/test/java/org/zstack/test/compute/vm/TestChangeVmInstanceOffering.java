package org.zstack.test.compute.vm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;

public class TestChangeVmInstanceOffering {
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

        InstanceOfferingInventory ioinv1 = deployer.instanceOfferings.get("TestInstanceOffering");
        InstanceOfferingInventory ioinv2 = deployer.instanceOfferings.get("TestInstanceOffering1");

        vm = api.changeInstanceOffering(vm.getUuid(), ioinv2.getUuid());
        // the vm is running, only instance offering uuid changed, capacity is not changed
        Assert.assertEquals(ioinv2.getUuid(), vm.getInstanceOfferingUuid());
        Assert.assertEquals(ioinv1.getCpuNum(), (int) vm.getCpuNum());
        Assert.assertEquals(ioinv1.getCpuSpeed(), (long) vm.getCpuSpeed());
        Assert.assertEquals(ioinv1.getMemorySize(), (long) vm.getMemorySize());

        vm = api.stopVmInstance(vm.getUuid());
        vm = api.startVmInstance(vm.getUuid());

        // after stop/start, the capacity changed
        Assert.assertEquals(ioinv2.getCpuNum(), (int) vm.getCpuNum());
        Assert.assertEquals(ioinv2.getCpuSpeed(), (long) vm.getCpuSpeed());
        Assert.assertEquals(ioinv2.getMemorySize(), (long) vm.getMemorySize());

        vm = api.stopVmInstance(vm.getUuid());

        vm = api.changeInstanceOffering(vm.getUuid(), ioinv1.getUuid());
        // the vm is stopped, the change take effect immediately
        Assert.assertEquals(ioinv1.getUuid(), vm.getInstanceOfferingUuid());
        Assert.assertEquals(ioinv1.getCpuNum(), (int) vm.getCpuNum());
        Assert.assertEquals(ioinv1.getCpuSpeed(), (long) vm.getCpuSpeed());
        Assert.assertEquals(ioinv1.getMemorySize(), (long) vm.getMemorySize());

        vm = api.startVmInstance(vm.getUuid());

        vm = api.changeInstanceOffering(vm.getUuid(), ioinv2.getUuid());
        // the vm is running, only instance offering uuid changed, capacity is not changed
        Assert.assertEquals(ioinv2.getUuid(), vm.getInstanceOfferingUuid());
        Assert.assertEquals(ioinv1.getCpuNum(), (int) vm.getCpuNum());
        Assert.assertEquals(ioinv1.getCpuSpeed(), (long) vm.getCpuSpeed());
        Assert.assertEquals(ioinv1.getMemorySize(), (long) vm.getMemorySize());

        vm = api.rebootVmInstance(vm.getUuid());
    }
}
