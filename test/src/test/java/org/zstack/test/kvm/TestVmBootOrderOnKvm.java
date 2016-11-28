package org.zstack.test.kvm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.compute.vm.VmSystemTags;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.vm.VmBootDevice;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.kvm.KVMAgentCommands.BootDev;
import org.zstack.kvm.KVMAgentCommands.StartVmCmd;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

/**
 * 1 create a vm from a data volume template
 * <p>
 * confirm the vm failed to create
 */
public class TestVmBootOrderOnKvm {
    CLogger logger = Utils.getLogger(TestVmBootOrderOnKvm.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    KVMSimulatorConfig config;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/kvm/TestCreateVmOnKvmIso.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(KVMSimulatorConfig.class);
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException {
        StartVmCmd scmd = config.startVmCmd;
        Assert.assertTrue(scmd.getBootDev().contains(BootDev.cdrom.toString()));

        VmInstanceInventory vm = deployer.vms.get("TestVm");
        List<String> order = api.getVmBootOrder(vm.getUuid(), null);
        Assert.assertEquals(1, order.size());
        Assert.assertEquals(VmBootDevice.HardDisk.toString(), order.get(0));

        vm = api.setVmBootOrder(vm.getUuid(), list(VmBootDevice.CdRom.toString(), VmBootDevice.HardDisk.toString()), null);
        Assert.assertTrue(VmSystemTags.BOOT_ORDER.hasTag(vm.getUuid()));
        vm = api.rebootVmInstance(vm.getUuid());
        scmd = config.startVmCmd;
        Assert.assertTrue(scmd.getBootDev().contains(BootDev.cdrom.toString()));
        Assert.assertTrue(scmd.getBootDev().contains(BootDev.hd.toString()));

        order = api.getVmBootOrder(vm.getUuid(), null);
        Assert.assertEquals(2, order.size());
        Assert.assertTrue(order.contains(VmBootDevice.CdRom.toString()));
        Assert.assertTrue(order.contains(VmBootDevice.HardDisk.toString()));

        api.stopVmInstance(vm.getUuid());
        api.startVmInstance(vm.getUuid());
        scmd = config.startVmCmd;
        Assert.assertTrue(scmd.getBootDev().contains(BootDev.cdrom.toString()));
        Assert.assertTrue(scmd.getBootDev().contains(BootDev.hd.toString()));

        config.rebootVmCmds.clear();
        vm = api.setVmBootOrder(vm.getUuid(), null, null);
        Assert.assertFalse(VmSystemTags.BOOT_ORDER.hasTag(vm.getUuid()));
        order = api.getVmBootOrder(vm.getUuid(), null);
        Assert.assertEquals(1, order.size());
        Assert.assertEquals(VmBootDevice.HardDisk.toString(), order.get(0));
        api.rebootVmInstance(vm.getUuid());
        scmd = config.startVmCmd;
        Assert.assertTrue(scmd.getBootDev().contains(BootDev.hd.toString()));

        api.stopVmInstance(vm.getUuid());
        api.startVmInstance(vm.getUuid());
        scmd = config.startVmCmd;
        Assert.assertTrue(scmd.getBootDev().contains(BootDev.hd.toString()));
    }
}
