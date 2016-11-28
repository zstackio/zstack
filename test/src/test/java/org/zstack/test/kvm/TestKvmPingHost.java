package org.zstack.test.kvm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.compute.host.HostGlobalConfig;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostStatus;
import org.zstack.header.host.HostVO;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.storage.backup.sftp.TestSftpBackupStorageDeleteImage2;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.concurrent.TimeUnit;

public class TestKvmPingHost {
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
        deployer = new Deployer("deployerXml/kvm/TestCreateVmOnKvm.xml", con);
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
    public void test() throws InterruptedException {
        HostGlobalConfig.AUTO_RECONNECT_ON_ERROR.updateValue(false);
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        HostInventory host = deployer.hosts.get("host1");
        HostGlobalConfig.PING_HOST_INTERVAL.updateValue(1);
        config.pingSuccess = false;
        TimeUnit.SECONDS.sleep(3);
        HostVO hvo = dbf.findByUuid(host.getUuid(), HostVO.class);
        Assert.assertEquals(HostStatus.Disconnected, hvo.getStatus());
        VmInstanceVO vmvo = dbf.findByUuid(vm.getUuid(), VmInstanceVO.class);
        Assert.assertEquals(VmInstanceState.Unknown, vmvo.getState());

        HostGlobalConfig.AUTO_RECONNECT_ON_ERROR.updateValue(true);
        config.pingSuccess = true;
        TimeUnit.SECONDS.sleep(3);
        hvo = dbf.findByUuid(host.getUuid(), HostVO.class);
        Assert.assertEquals(HostStatus.Connected, hvo.getStatus());
        vmvo = dbf.findByUuid(vm.getUuid(), VmInstanceVO.class);
        Assert.assertEquals(VmInstanceState.Running, vmvo.getState());

        HostGlobalConfig.AUTO_RECONNECT_ON_ERROR.updateValue(false);
        config.pingSuccess = false;
        TimeUnit.SECONDS.sleep(3);
        config.pingSuccess = true;
        TimeUnit.SECONDS.sleep(3);
        hvo = dbf.findByUuid(host.getUuid(), HostVO.class);
        Assert.assertEquals(HostStatus.Disconnected, hvo.getStatus());
        vmvo = dbf.findByUuid(vm.getUuid(), VmInstanceVO.class);
        Assert.assertEquals(VmInstanceState.Unknown, vmvo.getState());
    }
}
