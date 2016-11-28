package org.zstack.test.storage.snapshot;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.storage.snapshot.*;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeVO;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.simulator.kvm.VolumeSnapshotKvmSimulator;
import org.zstack.simulator.storage.primary.nfs.NfsPrimaryStorageSimulatorConfig;
import org.zstack.storage.snapshot.VolumeSnapshotGlobalConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/*
* 1. set max incremental snapshots to 2
* 2. take 6 snapshot from vm's root volume
* 3. delete second full snapshot
*
* confirm only one chain left
*
* 4. take a new snapshot
*
* confirm new chain created
*/
public class TestSnapshotOnKvm6 {
    CLogger logger = Utils.getLogger(TestSnapshotOnKvm6.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    NfsPrimaryStorageSimulatorConfig nfsConfig;
    KVMSimulatorConfig kvmConfig;
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
        kvmConfig = loader.getComponent(KVMSimulatorConfig.class);
        snapshotKvmSimulator = loader.getComponent(VolumeSnapshotKvmSimulator.class);
        session = api.loginAsAdmin();
        VolumeSnapshotGlobalConfig.MAX_INCREMENTAL_SNAPSHOT_NUM.updateValue(2);
    }

    private void fullSnapshot(VolumeSnapshotInventory inv, int distance, boolean isFullSnapshot) {
        Assert.assertEquals(VolumeSnapshotState.Enabled.toString(), inv.getState());
        Assert.assertEquals(VolumeSnapshotStatus.Ready.toString(), inv.getStatus());
        VolumeVO vol = dbf.findByUuid(inv.getVolumeUuid(), VolumeVO.class);
        VolumeSnapshotVO svo = dbf.findByUuid(inv.getUuid(), VolumeSnapshotVO.class);
        Assert.assertNotNull(svo);
        Assert.assertEquals(isFullSnapshot, svo.isFullSnapshot());
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
        String volUuid = vm.getRootVolumeUuid();
        VolumeSnapshotInventory inv = api.createSnapshot(volUuid);
        VolumeSnapshotInventory root = inv;
        // We don't take full snapshot for the first time (c.f. #794)
        fullSnapshot(inv, 0, false);

        inv = api.createSnapshot(volUuid);
        deltaSnapshot(inv, 1);

        inv = api.createSnapshot(volUuid);
        deltaSnapshot(inv, 2);

        VolumeSnapshotInventory inv2 = api.createSnapshot(volUuid);
        fullSnapshot(inv2, 0, true);

        inv = api.createSnapshot(volUuid);
        deltaSnapshot(inv, 1);

        inv = api.createSnapshot(volUuid);
        deltaSnapshot(inv, 2);

        long count = dbf.count(VolumeSnapshotTreeVO.class);
        Assert.assertEquals(2, count);

        api.deleteSnapshot(inv2.getUuid());

        Assert.assertEquals(3, nfsConfig.deleteCmds.size());
        Assert.assertEquals(1, kvmConfig.mergeSnapshotCmds.size());
        count = dbf.count(VolumeSnapshotTreeVO.class);
        Assert.assertEquals(1, count);

        SimpleQuery<VolumeSnapshotTreeVO> cq = dbf.createQuery(VolumeSnapshotTreeVO.class);
        cq.add(VolumeSnapshotTreeVO_.current, SimpleQuery.Op.EQ, true);
        count = cq.count();
        Assert.assertEquals(0, count);

        inv = api.createSnapshot(volUuid);
        VolumeSnapshotInventory root2 = inv;
        count = dbf.count(VolumeSnapshotTreeVO.class);
        Assert.assertEquals(2, count);

        count = dbf.count(VolumeSnapshotVO.class);
        Assert.assertEquals(4, count);

        VolumeSnapshotVO vo = dbf.findByUuid(inv.getUuid(), VolumeSnapshotVO.class);
        Assert.assertTrue(vo.isLatest());
        VolumeSnapshotTreeVO cvo = dbf.findByUuid(vo.getTreeUuid(), VolumeSnapshotTreeVO.class);
        Assert.assertTrue(cvo.isCurrent());

        snapshotKvmSimulator.validate(root);
        snapshotKvmSimulator.validate(root2);
    }

}
