package org.zstack.test.storage.snapshot;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.thread.AsyncThread;
import org.zstack.core.thread.ThreadGlobalProperty;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.storage.snapshot.VolumeSnapshotTreeVO;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO_;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.simulator.storage.primary.nfs.NfsPrimaryStorageSimulatorConfig;
import org.zstack.storage.snapshot.VolumeSnapshotGlobalConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/*
* create 100 snapshots
* delete all
*
* confirms delete success
*/
public class TestSnapshotOnKvm9 {
    CLogger logger = Utils.getLogger(TestSnapshotOnKvm9.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    int num = 100;
    CountDownLatch latch = new CountDownLatch(num);
    NfsPrimaryStorageSimulatorConfig nfsConfig;
    KVMSimulatorConfig kvmConfig;

    @Before
    public void setUp() throws Exception {
        Platform.getUuid();
        ThreadGlobalProperty.MAX_THREAD_NUM = 500;
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
        session = api.loginAsAdmin();
        VolumeSnapshotGlobalConfig.MAX_INCREMENTAL_SNAPSHOT_NUM.updateValue(1);
    }

    @AsyncThread
    private void create(String volUuid) throws ApiSenderException {
        try {
            api.createSnapshot(volUuid);
        } finally {
            latch.countDown();
        }
    }

    @AsyncThread
    private void delete(String suuid) throws ApiSenderException {
        try {
            api.deleteSnapshot(suuid);
        } finally {
            latch.countDown();
        }
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        String volUuid = vm.getRootVolumeUuid();

        for (int i = 0; i < num; i++) {
            create(volUuid);
        }

        latch.await(120, TimeUnit.SECONDS);
        long count = dbf.count(VolumeSnapshotVO.class);
        Assert.assertEquals(100, count);
        count = dbf.count(VolumeSnapshotTreeVO.class);
        Assert.assertEquals(50, count);
        SimpleQuery<VolumeSnapshotVO> q = dbf.createQuery(VolumeSnapshotVO.class);
        q.add(VolumeSnapshotVO_.latest, SimpleQuery.Op.EQ, true);
        count = q.count();
        Assert.assertEquals(50, count);

        latch = new CountDownLatch(num);
        q = dbf.createQuery(VolumeSnapshotVO.class);
        q.select(VolumeSnapshotVO_.uuid);
        List<String> uuids = q.listValue();
        for (String uuid : uuids) {
            delete(uuid);
        }
        latch.await(120, TimeUnit.SECONDS);
        count = dbf.count(VolumeSnapshotVO.class);
        Assert.assertEquals(0, count);
        count = dbf.count(VolumeSnapshotTreeVO.class);
        Assert.assertEquals(0, count);
        Assert.assertTrue(2 >= kvmConfig.mergeSnapshotCmds.size());
        Assert.assertEquals(num, nfsConfig.deleteCmds.size());
    }

}
