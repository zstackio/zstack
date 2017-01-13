package org.zstack.test.kvm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.host.HostInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.kvm.KVMAgentCommands.CheckVmStateCmd;
import org.zstack.kvm.KVMConstant.KvmVmState;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.storage.backup.sftp.TestSftpBackupStorageDeleteImage2;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.HashMap;
import java.util.Map;

/**
 * 1. create a vm
 * 2. set vm operations of start, stop, reboot, migrate all to fail
 * <p>
 * confirm after each failure, the vm's state is checked
 */
public class TestKvmFailureCheckState {
    CLogger logger = Utils.getLogger(TestSftpBackupStorageDeleteImage2.class);
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
        deployer = new Deployer("deployerXml/kvm/TestKvmFailureCheckState.xml", con);
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
    public void test() throws InterruptedException{
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        String hostUuid = vm.getHostUuid();
        HostInventory host2 = deployer.hosts.get("host2");

        Map<String, String> m = new HashMap<String, String>();
        m.put(vm.getUuid(), KvmVmState.Shutdown.toString());
        config.checkVmStatesConfig.put(hostUuid, m);

        config.stopVmSuccess = false;
        boolean s = false;
        try {
            api.stopVmInstance(vm.getUuid());
        } catch (ApiSenderException e) {
            s = true;
        }
        Assert.assertTrue(s);
        Assert.assertEquals(1, config.checkVmStateCmds.size());
        CheckVmStateCmd cmd = config.checkVmStateCmds.get(0);
        Assert.assertTrue(cmd.vmUuids.contains(vm.getUuid()));
        VmInstanceVO vmvo = dbf.findByUuid(vm.getUuid(), VmInstanceVO.class);
        Assert.assertEquals(VmInstanceState.Stopped, vmvo.getState());

        s = false;
        m.put(vm.getUuid(), KvmVmState.Running.toString());
        config.checkVmStateCmds.clear();
        config.startVmSuccess = false;
        try {
            api.startVmInstance(vm.getUuid());
        } catch (ApiSenderException e) {
            s = true;
        }
        Assert.assertTrue(s);
        Assert.assertEquals(1, config.checkVmStateCmds.size());
        cmd = config.checkVmStateCmds.get(0);
        Assert.assertTrue(cmd.vmUuids.contains(vm.getUuid()));
        vmvo = dbf.findByUuid(vm.getUuid(), VmInstanceVO.class);
        Thread.sleep(500);
        Assert.assertEquals(VmInstanceState.Running, vmvo.getState());

        s = false;
        m.put(vm.getUuid(), KvmVmState.Running.toString());
        config.checkVmStateCmds.clear();
        config.stopVmSuccess = false;
        try {
            api.rebootVmInstance(vm.getUuid());
        } catch (ApiSenderException e) {
            s = true;
        }
        Assert.assertTrue(s);
        Assert.assertEquals(1, config.checkVmStateCmds.size());
        cmd = config.checkVmStateCmds.get(0);
        Assert.assertTrue(cmd.vmUuids.contains(vm.getUuid()));
        vmvo = dbf.findByUuid(vm.getUuid(), VmInstanceVO.class);
        Assert.assertEquals(VmInstanceState.Running, vmvo.getState());

        s = false;
        m.put(vm.getUuid(), KvmVmState.Shutdown.toString());
        config.checkVmStateCmds.clear();
        config.migrateVmSuccess = false;
        try {
            api.migrateVmInstance(vm.getUuid(), host2.getUuid());
        } catch (ApiSenderException e) {
            s = true;
        }
        Assert.assertTrue(s);
        Assert.assertEquals(1, config.checkVmStateCmds.size());
        cmd = config.checkVmStateCmds.get(0);
        Assert.assertTrue(cmd.vmUuids.contains(vm.getUuid()));
        vmvo = dbf.findByUuid(vm.getUuid(), VmInstanceVO.class);
        Assert.assertEquals(VmInstanceState.Stopped, vmvo.getState());
    }

}
