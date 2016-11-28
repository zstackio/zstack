package org.zstack.test.storage.primary.nfs;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeVO;
import org.zstack.simulator.storage.primary.nfs.NfsPrimaryStorageSimulatorConfig;
import org.zstack.storage.volume.VolumeGlobalProperty;
import org.zstack.storage.volume.VolumeUpgradeExtension;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.storage.backup.sftp.TestSftpBackupStorageDeleteImage2;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.concurrent.TimeUnit;

public class TestNfsGetVolumeBaseImage {
    CLogger logger = Utils.getLogger(TestSftpBackupStorageDeleteImage2.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    VolumeUpgradeExtension volumeUpgradeExtension;
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
        volumeUpgradeExtension = loader.getComponent(VolumeUpgradeExtension.class);
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        String imageUuid = vm.getRootVolume().getRootImageUuid();
        config.getVolumeBaseImagePaths.put(vm.getRootVolumeUuid(), String.format("/%s.qcow2", imageUuid));
        VolumeVO vol = dbf.findByUuid(vm.getRootVolumeUuid(), VolumeVO.class);
        vol.setRootImageUuid(null);
        dbf.update(vol);

        VolumeGlobalProperty.ROOT_VOLUME_FIND_MISSING_IMAGE_UUID = true;
        volumeUpgradeExtension.start();

        PrimaryStorageInventory ps = deployer.primaryStorages.get("nfs");
        api.reconnectPrimaryStorage(ps.getUuid());
        TimeUnit.SECONDS.sleep(3);
        vol = dbf.findByUuid(vm.getRootVolumeUuid(), VolumeVO.class);
        Assert.assertEquals(imageUuid, vol.getRootImageUuid());
    }

}
