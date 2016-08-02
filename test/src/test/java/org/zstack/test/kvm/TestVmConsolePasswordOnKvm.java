package org.zstack.test.kvm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.compute.vm.VmSystemTags;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.DatabaseFacadeImpl;
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
 * Created by root on 7/29/16.
 */
public class TestVmConsolePasswordOnKvm {
    CLogger logger = Utils.getLogger(TestVmConsolePasswordOnKvm.class);
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
    public void test() throws ApiSenderException{
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        String consolePassword = api.getVmConsolePassword(vm.getUuid(), null);
        Assert.assertEquals(null, consolePassword);
        vm = api.setVmConsolePassword(vm.getUuid(), "password", null);
        Assert.assertTrue(VmSystemTags.CONSOLE_PASSWORD.hasTag(vm.getUuid()));
        consolePassword = api.getVmConsolePassword(vm.getUuid(), null);
        Assert.assertEquals("password",consolePassword);
        vm = api.rebootVmInstance(vm.getUuid());
        StartVmCmd scmd = config.startVmCmd;
        Assert.assertTrue(scmd.getConsolePassword().contains(consolePassword));
        vm = api.deleteVmConsolePassword(vm.getUuid(),null);
        Assert.assertFalse(VmSystemTags.CONSOLE_PASSWORD.hasTag(vm.getUuid()));
        consolePassword = api.getVmConsolePassword(vm.getUuid(), null);
        Assert.assertEquals(null,consolePassword);
        vm = api.rebootVmInstance(vm.getUuid());
        scmd = config.startVmCmd;
        Assert.assertTrue(scmd.getConsolePassword()==null);

    }
}
