package org.zstack.test.storage.snapshot;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.storage.snapshot.*;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeVO;
import org.zstack.simulator.kvm.VolumeSnapshotKvmSimulator;
import org.zstack.storage.snapshot.VolumeSnapshotGlobalConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/*
* 1. set max snapshots to 1
* 2. take 3 snapshot 1 2 3, confirm two trees are created
* 3. revert volume to snapshot 2 which is latest of tree 1
* 4. create snapshot 4
* 5. revert to snapshot 2 again
* 6. delete snapshot 2
* 7. create a new snapshot
*
* confirm tree.current are correctly set in each step of above
*/
public class TestSnapshotOnKvm40 {
    CLogger logger = Utils.getLogger(TestSnapshotOnKvm40.class);
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
        deployer = new Deployer("deployerXml/kvm/TestCreateVmOnKvm.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        snapshotKvmSimulator = loader.getComponent(VolumeSnapshotKvmSimulator.class);
        session = api.loginAsAdmin();
        VolumeSnapshotGlobalConfig.MAX_INCREMENTAL_SNAPSHOT_NUM.updateValue(1);
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

    private void isCurrentTree(VolumeSnapshotInventory inv) {
        SimpleQuery<VolumeSnapshotTreeVO> q = dbf.createQuery(VolumeSnapshotTreeVO.class);
        q.select(VolumeSnapshotTreeVO_.current);
        q.add(VolumeSnapshotTreeVO_.uuid, Op.EQ, inv.getTreeUuid());
        Boolean current = q.findValue();
        if (!current) {
            Assert.fail(String.format("tree[uuid:%s] is not the current", inv.getTreeUuid()));
        }

        q = dbf.createQuery(VolumeSnapshotTreeVO.class);
        q.add(VolumeSnapshotTreeVO_.uuid, Op.EQ, inv.getTreeUuid());
        q.add(VolumeSnapshotTreeVO_.current, Op.EQ, true);
        long count = q.count();
        Assert.assertEquals(1, count);
    }

    private void isLatest(VolumeSnapshotInventory inv) {
        SimpleQuery<VolumeSnapshotVO> q = dbf.createQuery(VolumeSnapshotVO.class);
        q.select(VolumeSnapshotVO_.latest);
        q.add(VolumeSnapshotVO_.uuid, Op.EQ, inv.getUuid());
        Boolean latest = q.findValue();
        if (!latest) {
            Assert.fail(String.format("snapshot[uuid:%s] is not the latest", inv.getUuid()));
        }

        q = dbf.createQuery(VolumeSnapshotVO.class);
        q.add(VolumeSnapshotVO_.treeUuid, Op.EQ, inv.getTreeUuid());
        q.add(VolumeSnapshotVO_.latest, Op.EQ, true);
        long count = q.count();
        Assert.assertEquals(1, count);
    }

    @Test
    public void test() throws ApiSenderException {
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        String volUuid = vm.getRootVolumeUuid();
        VolumeSnapshotInventory inv1 = api.createSnapshot(volUuid);
        fullSnapshot(inv1, 0);
        VolumeSnapshotInventory root = inv1;
        VolumeSnapshotInventory inv2 = api.createSnapshot(volUuid);
        deltaSnapshot(inv2, 1);
        isCurrentTree(inv2);
        isLatest(inv2);

        VolumeSnapshotInventory inv3 = api.createSnapshot(volUuid);
        long count = dbf.count(VolumeSnapshotTreeVO.class);
        Assert.assertEquals(2, count);
        isCurrentTree(inv3);
        isLatest(inv3);

        api.stopVmInstance(vm.getUuid());
        api.revertVolumeToSnapshot(inv2.getUuid());
        isCurrentTree(inv2);
        isLatest(inv2);

        VolumeSnapshotInventory inv4 = api.createSnapshot(volUuid);
        count = dbf.count(VolumeSnapshotTreeVO.class);
        Assert.assertEquals(3, count);
        isCurrentTree(inv4);
        isLatest(inv4);

        api.revertVolumeToSnapshot(inv2.getUuid());
        isCurrentTree(inv2);
        isLatest(inv2);

        api.deleteSnapshot(inv2.getUuid());

        VolumeSnapshotInventory inv5 = api.createSnapshot(volUuid);
        Assert.assertEquals(inv2.getTreeUuid(), inv5.getTreeUuid());
        isCurrentTree(inv5);
        isLatest(inv5);

        snapshotKvmSimulator.validate(root);
    }

}
