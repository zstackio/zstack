package org.zstack.test.storage.snapshot;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.storage.snapshot.*;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeVO;
import org.zstack.simulator.kvm.VolumeSnapshotKvmSimulator;
import org.zstack.simulator.storage.backup.sftp.SftpBackupStorageSimulatorConfig;
import org.zstack.simulator.storage.primary.nfs.NfsPrimaryStorageSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/*
* 1. take 4 snapshot from vm's root volume
* 2. backup snapshot 4
* 3. delete snapshot 3
*
* confirm snapshot 3,4 are deleted on backup storage but 1,2 are not
*/
public class TestSnapshotOnKvm19 {
    CLogger logger = Utils.getLogger(TestSnapshotOnKvm19.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    NfsPrimaryStorageSimulatorConfig nfsConfig;
    SftpBackupStorageSimulatorConfig bsConfig;
    VolumeSnapshotKvmSimulator snapshotKvmSimulator;

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
        nfsConfig = loader.getComponent(NfsPrimaryStorageSimulatorConfig.class);
        bsConfig = loader.getComponent(SftpBackupStorageSimulatorConfig.class);
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
    public void test() throws ApiSenderException, InterruptedException {
        BackupStorageInventory bs = deployer.backupStorages.get("sftp");
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        String volUuid = vm.getRootVolumeUuid();
        VolumeSnapshotInventory inv = api.createSnapshot(volUuid);
        VolumeSnapshotInventory root = inv;
        fullSnapshot(inv, 0);

        VolumeSnapshotInventory inv1 = api.createSnapshot(volUuid);
        deltaSnapshot(inv1, 1);

        VolumeSnapshotInventory inv2 = api.createSnapshot(volUuid);
        deltaSnapshot(inv2, 2);

        VolumeSnapshotInventory inv3 = api.createSnapshot(volUuid);
        deltaSnapshot(inv3, 3);

        inv3 = api.backupSnapshot(inv3.getUuid());
        VolumeSnapshotBackupStorageRefInventory ref = inv3.getBackupStorageRefs().get(0);
        Assert.assertEquals(bs.getUuid(), ref.getBackupStorageUuid());
        Assert.assertNotNull(ref.getInstallPath());

        SimpleQuery<VolumeSnapshotBackupStorageRefVO> q1 = dbf.createQuery(VolumeSnapshotBackupStorageRefVO.class);
        q1.add(VolumeSnapshotBackupStorageRefVO_.backupStorageUuid, SimpleQuery.Op.NOT_NULL);
        long count = q1.count();
        Assert.assertEquals(4, count);

        SimpleQuery<VolumeSnapshotBackupStorageRefVO> q2 = dbf.createQuery(VolumeSnapshotBackupStorageRefVO.class);
        q2.add(VolumeSnapshotBackupStorageRefVO_.installPath, SimpleQuery.Op.NOT_NULL);
        count = q2.count();
        Assert.assertEquals(4, count);

        api.deleteSnapshot(inv2.getUuid());

        Assert.assertEquals(2, bsConfig.deleteCmds.size());

        VolumeSnapshotVO vo = dbf.findByUuid(inv.getUuid(), VolumeSnapshotVO.class);
        VolumeSnapshotBackupStorageRefVO refvo = vo.getBackupStorageRefs().iterator().next();
        Assert.assertNotNull(refvo.getBackupStorageUuid());
        Assert.assertNotNull(refvo.getInstallPath());

        vo = dbf.findByUuid(inv1.getUuid(), VolumeSnapshotVO.class);
        refvo = vo.getBackupStorageRefs().iterator().next();
        Assert.assertNotNull(refvo.getBackupStorageUuid());
        Assert.assertNotNull(refvo.getInstallPath());

        snapshotKvmSimulator.validate(root);
    }
}
