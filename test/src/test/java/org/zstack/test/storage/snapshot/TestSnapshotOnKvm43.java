package org.zstack.test.storage.snapshot;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.storage.snapshot.*;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeType;
import org.zstack.header.volume.VolumeVO;
import org.zstack.simulator.kvm.VolumeSnapshotKvmSimulator;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

/*
* 1. take 4 snapshot from vm's data volume
* 2. backup to sftp
* 3. delete snapshot1 from sftp
*
* confirm snapshots are deleted and backup storage capacity are returned
*/
public class TestSnapshotOnKvm43 {
    CLogger logger = Utils.getLogger(TestSnapshotOnKvm43.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    VolumeSnapshotKvmSimulator snapshotKvmSimulator;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/kvm/TestTakeSnapshotOnKvm43.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        snapshotKvmSimulator = loader.getComponent(VolumeSnapshotKvmSimulator.class);
        session = api.loginAsAdmin();
    }

    private void fullSnapshot(VolumeSnapshotInventory inv, int distance) {
        Assert.assertEquals(VolumeSnapshotState.Enabled.toString(), inv.getState());
        Assert.assertEquals(VolumeSnapshotStatus.Ready.toString(), inv.getStatus());
        VolumeVO vol = dbf.findByUuid(inv.getVolumeUuid(), VolumeVO.class);
        VolumeSnapshotVO svo = dbf.findByUuid(inv.getUuid(), VolumeSnapshotVO.class);
        Assert.assertNotNull(svo);
        Assert.assertFalse(svo.isFullSnapshot());
        Assert.assertTrue(svo.isLatest());
        Assert.assertNull(svo.getParentUuid());
        Assert.assertEquals(distance, svo.getDistance());
        Assert.assertEquals(vol.getPrimaryStorageUuid(), svo.getPrimaryStorageUuid());
        Assert.assertNotNull(svo.getPrimaryStorageInstallPath());
        VolumeSnapshotTreeVO cvo = dbf.findByUuid(svo.getTreeUuid(), VolumeSnapshotTreeVO.class);
        Assert.assertNotNull(cvo);
        Assert.assertTrue(cvo.isCurrent());
    }

    private void deltaSnapshot(VolumeSnapshotInventory inv, int distance) {
        Assert.assertEquals(VolumeSnapshotState.Enabled.toString(), inv.getState());
        Assert.assertEquals(VolumeSnapshotStatus.Ready.toString(), inv.getStatus());
        VolumeVO vol = dbf.findByUuid(inv.getVolumeUuid(), VolumeVO.class);
        VolumeSnapshotVO svo = dbf.findByUuid(inv.getUuid(), VolumeSnapshotVO.class);
        Assert.assertNotNull(svo);
        Assert.assertFalse(svo.isFullSnapshot());
        Assert.assertTrue(svo.isLatest());
        Assert.assertNotNull(svo.getParentUuid());
        Assert.assertEquals(distance, svo.getDistance());
        Assert.assertEquals(vol.getPrimaryStorageUuid(), svo.getPrimaryStorageUuid());
        Assert.assertNotNull(svo.getPrimaryStorageInstallPath());
        VolumeSnapshotTreeVO cvo = dbf.findByUuid(svo.getTreeUuid(), VolumeSnapshotTreeVO.class);
        Assert.assertNotNull(cvo);
        Assert.assertTrue(cvo.isCurrent());
        Assert.assertEquals(svo.getTreeUuid(), cvo.getUuid());
    }

    @Test
    public void test() throws ApiSenderException {
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        VolumeInventory dataVol = CollectionUtils.find(vm.getAllVolumes(), new Function<VolumeInventory, VolumeInventory>() {
            @Override
            public VolumeInventory call(VolumeInventory arg) {
                if (arg.getType().equals(VolumeType.Data.toString())) {
                    return arg;
                }
                return null;
            }
        });

        String volUuid = dataVol.getUuid();
        VolumeSnapshotInventory inv1 = api.createSnapshot(volUuid);
        fullSnapshot(inv1, 0);

        VolumeSnapshotInventory inv = api.createSnapshot(volUuid);
        deltaSnapshot(inv, 1);

        inv = api.createSnapshot(volUuid);
        deltaSnapshot(inv, 2);

        inv = api.createSnapshot(volUuid);
        deltaSnapshot(inv, 3);

        BackupStorageInventory sftp = deployer.backupStorages.get("sftp");
        api.backupSnapshot(inv.getUuid());

        api.deleteSnapshotFromBackupStorage(inv1.getUuid(), sftp.getUuid());

        BackupStorageVO sftpvo = dbf.findByUuid(sftp.getUuid(), BackupStorageVO.class);
        Assert.assertEquals(sftp.getAvailableCapacity(), sftpvo.getAvailableCapacity());
    }
}
