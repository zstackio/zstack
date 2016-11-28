package org.zstack.test.storage.primary.local;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.host.HostInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.storage.primary.PrimaryStorageOverProvisioningManager;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.kvm.KVMHostVO;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.storage.primary.local.LocalStorageKvmBackend.CheckMd5sumCmd;
import org.zstack.storage.primary.local.LocalStorageKvmBackend.GetBackingFileCmd;
import org.zstack.storage.primary.local.LocalStorageKvmBackend.GetMd5Cmd;
import org.zstack.storage.primary.local.LocalStorageKvmMigrateVmFlow.CopyBitsFromRemoteCmd;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig.Capacity;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.logging.CLogger;

/**
 * 1. delete the image
 * 2. migrate a vm with storage
 * <p>
 * confirm the backing file is migrated
 */
public class TestLocalStorage38 {
    CLogger logger = Utils.getLogger(TestLocalStorage38.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    KVMSimulatorConfig kconfig;
    LocalStorageSimulatorConfig config;
    PrimaryStorageOverProvisioningManager ratioMgr;
    long totalSize = SizeUnit.GIGABYTE.toByte(100);

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/localStorage/TestLocalStorage28.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("localStorageSimulator.xml");
        deployer.addSpringConfig("localStorage.xml");
        deployer.load();

        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(LocalStorageSimulatorConfig.class);
        kconfig = loader.getComponent(KVMSimulatorConfig.class);
        ratioMgr = loader.getComponent(PrimaryStorageOverProvisioningManager.class);

        Capacity c = new Capacity();
        c.total = totalSize;
        c.avail = totalSize;

        config.capacityMap.put("host1", c);
        config.capacityMap.put("host2", c);

        deployer.build();
        api = deployer.getApi();
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        HostInventory host2 = deployer.hosts.get("host2");
        VmInstanceInventory vm = deployer.vms.get("TestVm");

        ImageInventory image = deployer.images.get("TestImage");
        api.deleteImage(image.getUuid());

        config.backingFilePath = image.getBackupStorageRefs().get(0).getInstallPath();
        config.backingFileSize = image.getSize();
        config.checkBitsSuccess = false;
        VolumeInventory root = vm.getRootVolume();

        api.migrateVmInstance(vm.getUuid(), host2.getUuid());
        Assert.assertEquals(1, config.getBackingFileCmds.size());
        GetBackingFileCmd getBackingFileCmd = config.getBackingFileCmds.get(0);
        Assert.assertEquals(root.getInstallPath(), getBackingFileCmd.path);

        Assert.assertEquals(1, config.getMd5Cmds.size());
        GetMd5Cmd getMd5Cmd = config.getMd5Cmds.get(0);
        Assert.assertEquals(config.backingFilePath, getMd5Cmd.md5s.get(0).path);

        Assert.assertEquals(1, config.copyBitsFromRemoteCmds.size());
        CopyBitsFromRemoteCmd copyBitsFromRemoteCmd = config.copyBitsFromRemoteCmds.get(0);
        KVMHostVO kvm = dbf.findByUuid(host2.getUuid(), KVMHostVO.class);
        Assert.assertEquals(kvm.getManagementIp(), copyBitsFromRemoteCmd.dstIp);
        Assert.assertEquals(kvm.getUsername(), copyBitsFromRemoteCmd.dstUsername);
        Assert.assertEquals(kvm.getPassword(), copyBitsFromRemoteCmd.dstPassword);
        Assert.assertEquals(1, copyBitsFromRemoteCmd.paths.size());
        Assert.assertEquals(config.backingFilePath, copyBitsFromRemoteCmd.paths.get(0));

        Assert.assertFalse(config.checkMd5sumCmds.isEmpty());
        CheckMd5sumCmd checkMd5sumCmd = config.checkMd5sumCmds.get(0);
        Assert.assertEquals(config.backingFilePath, checkMd5sumCmd.md5s.get(0).path);
    }
}
