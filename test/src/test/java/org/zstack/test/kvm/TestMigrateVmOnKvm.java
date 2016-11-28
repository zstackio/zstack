package org.zstack.test.kvm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.allocator.HostCapacityVO;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostVO;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.kvm.KVMAgentCommands.DeleteVmConsoleFirewallCmd;
import org.zstack.kvm.KVMAgentCommands.HardenVmConsoleCmd;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.storage.backup.sftp.TestSftpBackupStorageDeleteImage2;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

public class TestMigrateVmOnKvm {
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
        deployer = new Deployer("deployerXml/kvm/TestMigrateVmOnKvm.xml", con);
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
        final VmInstanceInventory vm = deployer.vms.get("TestVm");
        HostInventory target = CollectionUtils.find(deployer.hosts.values(), new Function<HostInventory, HostInventory>() {
            @Override
            public HostInventory call(HostInventory arg) {
                if (!arg.getUuid().equals(vm.getHostUuid())) {
                    return arg;
                }
                return null;
            }
        });

        String lastHostUuid = vm.getHostUuid();
        HostVO lastHost = dbf.findByUuid(lastHostUuid, HostVO.class);

        VmInstanceInventory vminv = api.migrateVmInstance(vm.getUuid(), target.getUuid());
        Assert.assertFalse(config.migrateVmCmds.isEmpty());
        Assert.assertEquals(target.getUuid(), vminv.getHostUuid());
        Assert.assertEquals(lastHostUuid, vminv.getLastHostUuid());
        Assert.assertEquals(VmInstanceState.Running.toString(), vminv.getState());
        HostCapacityVO cvo = dbf.findByUuid(lastHostUuid, HostCapacityVO.class);
        Assert.assertEquals(0, cvo.getUsedCpu());
        Assert.assertEquals(0, cvo.getUsedMemory());

        VmInstanceVO vmvo = dbf.findByUuid(vm.getUuid(), VmInstanceVO.class);

        Assert.assertEquals(1, config.hardenVmConsoleCmds.size());
        HardenVmConsoleCmd cmd = config.hardenVmConsoleCmds.get(0);
        Assert.assertEquals(vmvo.getInternalId(), cmd.vmInternalId.longValue());
        Assert.assertEquals(vmvo.getUuid(), cmd.vmUuid);
        Assert.assertEquals(target.getManagementIp(), cmd.hostManagementIp);

        Assert.assertEquals(1, config.deleteVmConsoleFirewallCmds.size());
        DeleteVmConsoleFirewallCmd dcmd = config.deleteVmConsoleFirewallCmds.get(0);
        Assert.assertEquals(vmvo.getInternalId(), dcmd.vmInternalId.longValue());
        Assert.assertEquals(vmvo.getUuid(), dcmd.vmUuid);
        Assert.assertEquals(lastHost.getManagementIp(), dcmd.hostManagementIp);
    }
}
