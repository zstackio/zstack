package org.zstack.test.kvm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.storage.primary.PrimaryStorageCapacityVO;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeDeletionPolicyManager.VolumeDeletionPolicy;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeStatus;
import org.zstack.header.volume.VolumeVO;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.simulator.storage.primary.nfs.NfsPrimaryStorageSimulatorConfig;
import org.zstack.storage.primary.nfs.NfsPrimaryStorageKVMBackendCommands.DeleteCmd;
import org.zstack.storage.volume.VolumeGlobalConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.storage.backup.sftp.TestSftpBackupStorageDeleteImage2;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.concurrent.TimeUnit;

public class TestDeleteDataVolumeOnKvm {
    CLogger logger = Utils.getLogger(TestSftpBackupStorageDeleteImage2.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    KVMSimulatorConfig config;
    NfsPrimaryStorageSimulatorConfig nconfig;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/kvm/TestAttachVolumeToVmOnKvm.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(KVMSimulatorConfig.class);
        nconfig = loader.getComponent(NfsPrimaryStorageSimulatorConfig.class);
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        DiskOfferingInventory dinv = deployer.diskOfferings.get("DataOffering");
        VolumeInventory vol1 = api.createDataVolume("d1", dinv.getUuid());
        VolumeInventory vol2 = api.createDataVolume("d2", dinv.getUuid());
        VolumeInventory vol3 = api.createDataVolume("d3", dinv.getUuid());
        vol1 = api.attachVolumeToVm(vm.getUuid(), vol1.getUuid());
        vol2 = api.attachVolumeToVm(vm.getUuid(), vol2.getUuid());
        vol3 = api.attachVolumeToVm(vm.getUuid(), vol3.getUuid());

        api.deleteDataVolume(vol1.getUuid());
        VolumeVO volvo1 = dbf.findByUuid(vol1.getUuid(), VolumeVO.class);
        Assert.assertNotNull(volvo1);
        Assert.assertEquals(VolumeStatus.Deleted, volvo1.getStatus());
        Assert.assertEquals(vol1.getInstallPath(), volvo1.getInstallPath());
        Assert.assertTrue(nconfig.deleteCmds.isEmpty());

        vol1 = api.recoverVolume(vol1.getUuid(), null);
        volvo1 = dbf.findByUuid(vol1.getUuid(), VolumeVO.class);
        Assert.assertEquals(VolumeStatus.Ready, volvo1.getStatus());
        api.attachVolumeToVm(vm.getUuid(), vol1.getUuid());

        PrimaryStorageInventory ps = deployer.primaryStorages.get("nfs");
        PrimaryStorageCapacityVO cap = dbf.findByUuid(ps.getUuid(), PrimaryStorageCapacityVO.class);

        VolumeGlobalConfig.VOLUME_EXPUNGE_PERIOD.updateValue(1);
        VolumeGlobalConfig.VOLUME_EXPUNGE_INTERVAL.updateValue(1);
        api.deleteDataVolume(vol1.getUuid());
        TimeUnit.SECONDS.sleep(3);
        volvo1 = dbf.findByUuid(vol1.getUuid(), VolumeVO.class);
        Assert.assertNull(volvo1);
        Assert.assertEquals(1, nconfig.deleteCmds.size());
        DeleteCmd cmd = nconfig.deleteCmds.get(0);
        Assert.assertTrue(vol1.getInstallPath().contains(cmd.getInstallPath()));

        PrimaryStorageCapacityVO cap1 = dbf.findByUuid(ps.getUuid(), PrimaryStorageCapacityVO.class);
        Assert.assertEquals(cap1.getAvailableCapacity(), cap.getAvailableCapacity() + vol1.getSize());

        nconfig.deleteCmds.clear();
        VolumeGlobalConfig.VOLUME_DELETION_POLICY.updateValue(VolumeDeletionPolicy.Direct);
        api.deleteDataVolume(vol2.getUuid());
        VolumeVO volvo2 = dbf.findByUuid(vol2.getUuid(), VolumeVO.class);
        Assert.assertNull(volvo2);
        Assert.assertEquals(1, nconfig.deleteCmds.size());
        cmd = nconfig.deleteCmds.get(0);
        Assert.assertTrue(vol2.getInstallPath().contains(cmd.getInstallPath()));

        PrimaryStorageCapacityVO cap2 = dbf.findByUuid(ps.getUuid(), PrimaryStorageCapacityVO.class);
        Assert.assertEquals(cap2.getAvailableCapacity(), cap1.getAvailableCapacity() + vol2.getSize());

        nconfig.deleteCmds.clear();
        VolumeGlobalConfig.VOLUME_DELETION_POLICY.updateValue(VolumeDeletionPolicy.Never);
        api.deleteDataVolume(vol3.getUuid());
        TimeUnit.SECONDS.sleep(3);
        VolumeVO volvo3 = dbf.findByUuid(vol3.getUuid(), VolumeVO.class);
        Assert.assertNotNull(volvo3);
        Assert.assertEquals(VolumeStatus.Deleted, volvo3.getStatus());
        Assert.assertEquals(vol3.getInstallPath(), volvo3.getInstallPath());
        Assert.assertTrue(nconfig.deleteCmds.isEmpty());

        VolumeGlobalConfig.VOLUME_DELETION_POLICY.updateValue(VolumeDeletionPolicy.Delay);
        TimeUnit.SECONDS.sleep(3);
        volvo3 = dbf.findByUuid(vol3.getUuid(), VolumeVO.class);
        Assert.assertNull(volvo3);
        Assert.assertEquals(1, nconfig.deleteCmds.size());
        cmd = nconfig.deleteCmds.get(0);
        Assert.assertTrue(vol3.getInstallPath().contains(cmd.getInstallPath()));

        PrimaryStorageCapacityVO cap3 = dbf.findByUuid(ps.getUuid(), PrimaryStorageCapacityVO.class);
        Assert.assertEquals(cap3.getAvailableCapacity(), cap2.getAvailableCapacity() + vol3.getSize());

        VolumeGlobalConfig.VOLUME_DELETION_POLICY.updateValue(VolumeDeletionPolicy.Never);
        nconfig.deleteCmds.clear();
        VolumeInventory vol4 = api.createDataVolume("d4", dinv.getUuid());
        api.deleteDataVolume(vol4.getUuid());
        TimeUnit.SECONDS.sleep(3);
        VolumeVO volvo4 = dbf.findByUuid(vol4.getUuid(), VolumeVO.class);
        Assert.assertNotNull(volvo4);
        Assert.assertTrue(nconfig.deleteCmds.isEmpty());

        vol4 = api.recoverVolume(vol4.getUuid(), null);
        api.attachVolumeToVm(vm.getUuid(), vol4.getUuid());

        VolumeGlobalConfig.VOLUME_DELETION_POLICY.updateValue(VolumeDeletionPolicy.Delay);
        VolumeInventory vol5 = api.createDataVolume("d5", dinv.getUuid());
        api.deleteDataVolume(vol5.getUuid());
        TimeUnit.SECONDS.sleep(3);
        VolumeVO volvo5 = dbf.findByUuid(vol5.getUuid(), VolumeVO.class);
        Assert.assertNull(volvo5);
        Assert.assertTrue(nconfig.deleteCmds.isEmpty());

        VolumeGlobalConfig.VOLUME_EXPUNGE_PERIOD.updateValue(1000);
        VolumeGlobalConfig.VOLUME_EXPUNGE_INTERVAL.updateValue(1000);
        nconfig.deleteCmds.clear();
        VolumeInventory vol6 = api.createDataVolume("d6", dinv.getUuid());
        vol6 = api.attachVolumeToVm(vm.getUuid(), vol6.getUuid());
        api.deleteDataVolume(vol6.getUuid());
        Assert.assertTrue(nconfig.deleteCmds.isEmpty());
        api.expungeDataVolume(vol6.getUuid(), null);

        VolumeVO volvo6 = dbf.findByUuid(vol6.getUuid(), VolumeVO.class);
        Assert.assertNull(volvo6);
        Assert.assertEquals(1, nconfig.deleteCmds.size());
        cmd = nconfig.deleteCmds.get(0);
        Assert.assertTrue(vol6.getInstallPath().contains(cmd.getInstallPath()));
    }
}
