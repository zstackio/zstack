package org.zstack.test.storage.primary.nfs;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.image.ImageUpgradeExtension;
import org.zstack.simulator.storage.primary.nfs.NfsPrimaryStorageSimulatorConfig;
import org.zstack.storage.primary.nfs.NfsPrimaryStorageKVMBackendCommands.UpdateMountPointCmd;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.storage.backup.sftp.TestSftpBackupStorageDeleteImage2;
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.logging.CLogger;

public class TestNfsUpdateUrl {
    CLogger logger = Utils.getLogger(TestSftpBackupStorageDeleteImage2.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    ImageUpgradeExtension imgUpgradeExtension;
    NfsPrimaryStorageSimulatorConfig config;

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
        config = loader.getComponent(NfsPrimaryStorageSimulatorConfig.class);
        imgUpgradeExtension = loader.getComponent(ImageUpgradeExtension.class);
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        String url = "localhost:/test";
        PrimaryStorageInventory nfs = deployer.primaryStorages.get("nfs");
        String oldUrl = nfs.getUrl();

        nfs.setUrl(url);
        boolean s = false;
        try {
            api.updatePrimaryStorage(nfs);
        } catch (ApiSenderException e) {
            s = true;
        }
        Assert.assertTrue(s);

        VmInstanceInventory vm = deployer.vms.get("TestVm");
        api.stopVmInstance(vm.getUuid());

        config.totalCapacity = SizeUnit.GIGABYTE.toByte(200);
        config.availableCapacity = SizeUnit.GIGABYTE.toByte(10);

        api.updatePrimaryStorage(nfs);
        Assert.assertEquals(1, config.updateMountPointCmds.size());
        UpdateMountPointCmd cmd = config.updateMountPointCmds.get(0);
        Assert.assertEquals(nfs.getUuid(), cmd.getUuid());
        Assert.assertEquals(oldUrl, cmd.oldMountPoint);
        Assert.assertEquals(url, cmd.newMountPoint);
        Assert.assertEquals(nfs.getMountPath(), cmd.mountPath);

        PrimaryStorageVO ps = dbf.findByUuid(nfs.getUuid(), PrimaryStorageVO.class);
        Assert.assertEquals(url, ps.getUrl());
        Assert.assertEquals(config.totalCapacity, ps.getCapacity().getTotalCapacity());
        Assert.assertEquals(config.totalCapacity, ps.getCapacity().getTotalPhysicalCapacity());
        Assert.assertEquals(config.availableCapacity, ps.getCapacity().getAvailablePhysicalCapacity());
    }
}
