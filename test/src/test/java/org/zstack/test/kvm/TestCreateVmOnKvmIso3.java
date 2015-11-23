package org.zstack.test.kvm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.compute.vm.VmSystemTags;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.storage.primary.ImageCacheVO;
import org.zstack.header.storage.primary.ImageCacheVO_;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.kvm.KVMAgentCommands.BootDev;
import org.zstack.kvm.KVMAgentCommands.StartVmCmd;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.simulator.storage.backup.sftp.SftpBackupStorageSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.storage.backup.sftp.TestSftpBackupStorageDeleteImage2;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/**
 * 1. create a vm from ISO
 * 2. stop the vm
 * 3. start the vm
 *
 * confirm the vm still started from ISO
 *
 * 4. detach the ISO
 *
 * confirm the ISO detached
 *
 * 5. stop the vm
 * 6. start the vm
 *
 * confirm the vm not started from ISO
 */
public class TestCreateVmOnKvmIso3 {
    CLogger logger = Utils.getLogger(TestSftpBackupStorageDeleteImage2.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    SftpBackupStorageSimulatorConfig config;
    KVMSimulatorConfig kconfig;

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
        config = loader.getComponent(SftpBackupStorageSimulatorConfig.class);
        kconfig = loader.getComponent(KVMSimulatorConfig.class);
        session = api.loginAsAdmin();
    }
    
	@Test
	public void test() throws ApiSenderException {
        VmInstanceInventory vm = deployer.vms.get("TestVm");

        api.stopVmInstance(vm.getUuid());
        api.startVmInstance(vm.getUuid());
        StartVmCmd scmd = kconfig.startVmCmd;
        Assert.assertNotNull(scmd);
        Assert.assertNotNull(scmd.getBootIso());
        Assert.assertEquals(BootDev.cdrom.toString(), scmd.getBootDev().get(0));

        api.detachIso(vm.getUuid(), null);
        Assert.assertFalse(VmSystemTags.ISO.hasTag(vm.getUuid()));

        api.stopVmInstance(vm.getUuid());
        api.startVmInstance(vm.getUuid());
        scmd = kconfig.startVmCmd;
        Assert.assertNotNull(scmd);
        Assert.assertNull(scmd.getBootIso());
        Assert.assertEquals(BootDev.hd.toString(), scmd.getBootDev().get(0));
    }
}
