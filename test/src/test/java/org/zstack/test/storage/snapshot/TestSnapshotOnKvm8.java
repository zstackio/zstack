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
import org.zstack.simulator.kvm.VolumeSnapshotKvmSimulator;
import org.zstack.simulator.storage.primary.nfs.NfsPrimaryStorageSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/*
* 1. create 3 snapshots: 0, 1, 2
* 2. revert volume to snapshot 1
* 3. takes other 5 snapshots
* 4. delete snapshot 2
*
* confirm deletion success and chain is ok
*/
public class TestSnapshotOnKvm8 {
    CLogger logger = Utils.getLogger(TestSnapshotOnKvm8.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    NfsPrimaryStorageSimulatorConfig nfsConfig;
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
        String volUuid = vm.getRootVolumeUuid();
        VolumeSnapshotInventory inv = api.createSnapshot(volUuid);
        VolumeSnapshotInventory root = inv;
        fullSnapshot(inv, 0);

        inv = api.createSnapshot(volUuid);
        deltaSnapshot(inv, 1);

        VolumeSnapshotInventory inv0 = api.createSnapshot(volUuid);
        deltaSnapshot(inv0, 2);

        api.stopVmInstance(vm.getUuid());

        api.revertVolumeToSnapshot(inv.getUuid());
        VolumeSnapshotVO vo1 = dbf.findByUuid(inv.getUuid(), VolumeSnapshotVO.class);
        Assert.assertTrue(vo1.isLatest());
        vo1 = dbf.findByUuid(inv0.getUuid(), VolumeSnapshotVO.class);
        Assert.assertFalse(vo1.isLatest());

        VolumeSnapshotInventory inv1 = api.createSnapshot(volUuid);
        deltaSnapshot(inv1, 2);

        VolumeSnapshotInventory inv2 = api.createSnapshot(volUuid);
        deltaSnapshot(inv2, 3);

        inv = api.createSnapshot(volUuid);
        deltaSnapshot(inv, 4);

        inv = api.createSnapshot(volUuid);
        deltaSnapshot(inv, 5);

        inv = api.createSnapshot(volUuid);
        deltaSnapshot(inv, 6);

        long count = dbf.count(VolumeSnapshotTreeVO.class);
        Assert.assertEquals(1, count);

        api.deleteSnapshot(inv0.getUuid());

        count = dbf.count(VolumeSnapshotVO.class);
        Assert.assertEquals(7, count);

        SimpleQuery<VolumeSnapshotVO> q = dbf.createQuery(VolumeSnapshotVO.class);
        q.add(VolumeSnapshotVO_.treeUuid, SimpleQuery.Op.EQ, vo1.getTreeUuid());
        q.add(VolumeSnapshotVO_.latest, SimpleQuery.Op.EQ, true);
        count = q.count();
        Assert.assertEquals(1, count);

        VolumeSnapshotVO vo = dbf.findByUuid(inv.getUuid(), VolumeSnapshotVO.class);
        Assert.assertTrue(vo.isLatest());
        VolumeSnapshotTreeVO cvo = dbf.findByUuid(vo.getTreeUuid(), VolumeSnapshotTreeVO.class);
        Assert.assertTrue(cvo.isCurrent());
        Assert.assertEquals(1, nfsConfig.deleteCmds.size());

        snapshotKvmSimulator.validate(root);
    }

}
