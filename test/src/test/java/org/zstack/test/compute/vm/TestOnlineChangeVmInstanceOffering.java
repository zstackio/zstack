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
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.data.SizeUnit;

/**
 * Created by luchukun on 8/9/16.
 */
public class TestOnlineChangeVmInstanceOffering {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    //KVMSimulatorConfig config;

    @Before
    public void setUp() throws Exception {
        WebBeanConstructor con = new WebBeanConstructor();
        DBUtil.reDeployDB();

        deployer = new Deployer("deployerXml/kvm/TestCreateVmOnKvmIso.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        //deployer = new Deployer("deployerXml/vm/TestChangeVmInstanceOffering.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        //config = loader.getComponent(KVMSimulatorConfig.class);

    }

    @Test
    public void test() throws ApiSenderException {
        VmInstanceInventory vm = deployer.vms.get("TestVm");

        InstanceOfferingInventory inv = new InstanceOfferingInventory();
        inv.setName("inv");
        inv.setCpuNum(1);
        inv.setCpuSpeed(1000);
        inv.setMemorySize(SizeUnit.GIGABYTE.toByte(1));
        inv.setDescription("########################################################");
        inv = api.addInstanceOffering(inv);

        InstanceOfferingInventory inv1 = new InstanceOfferingInventory();
        inv1.setName("inv1");
        inv1.setCpuNum(2);
        inv1.setCpuSpeed(1000);
        inv1.setMemorySize(SizeUnit.GIGABYTE.toByte(2));
        inv1.setDescription("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
        inv1 = api.addInstanceOffering(inv1);

        vm = api.changeInstanceOffering(vm.getUuid(), inv.getUuid());
        Assert.assertEquals(inv.getUuid(), vm.getInstanceOfferingUuid());

        vm = api.stopVmInstance(vm.getUuid());
        vm = api.startVmInstance(vm.getUuid());

        Assert.assertEquals(inv.getCpuNum(), (int) vm.getCpuNum());
        Assert.assertEquals(inv.getCpuSpeed(), (long) vm.getCpuSpeed());
        Assert.assertEquals(inv.getMemorySize(), (long) vm.getMemorySize());
        vm = api.changeInstanceOffering(vm.getUuid(), inv1.getUuid());
        Assert.assertEquals(inv1.getCpuNum(), (int) vm.getCpuNum());
        Assert.assertEquals(inv1.getCpuSpeed(), (long) vm.getCpuSpeed());
        Assert.assertEquals(inv1.getMemorySize(), (long) vm.getMemorySize());
    }
}
