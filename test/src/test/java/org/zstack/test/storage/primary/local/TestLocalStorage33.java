package org.zstack.test.storage.primary.local;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.host.HostInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.storage.primary.PrimaryStorageOverProvisioningManager;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO_;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeType;
import org.zstack.storage.primary.local.*;
import org.zstack.storage.primary.local.LocalStorageKvmBackend.DeleteBitsCmd;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig.Capacity;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.function.Function;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 1. use local storage
 * 2. create a vm with data volume
 * 3. stop the vm and detach the data volume
 * 4. migrate the data volume with snapshots, and fails it on purpose
 * <p>
 * confirm all rollback happened
 */
public class TestLocalStorage33 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    LocalStorageSimulatorConfig config;
    PrimaryStorageOverProvisioningManager psRatioMgr;
    long totalSize = SizeUnit.GIGABYTE.toByte(100);

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/localStorage/TestLocalStorage32.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("localStorageSimulator.xml");
        deployer.addSpringConfig("localStorage.xml");
        deployer.load();

        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(LocalStorageSimulatorConfig.class);
        psRatioMgr = loader.getComponent(PrimaryStorageOverProvisioningManager.class);

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
        PrimaryStorageInventory local = deployer.primaryStorages.get("local");
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        api.stopVmInstance(vm.getUuid());

        VolumeInventory data = CollectionUtils.find(vm.getAllVolumes(), new Function<VolumeInventory, VolumeInventory>() {
            @Override
            public VolumeInventory call(VolumeInventory arg) {
                return VolumeType.Data.toString().equals(arg.getType()) ? arg : null;
            }
        });

        api.detachVolumeFromVm(data.getUuid());

        HostInventory host1 = deployer.hosts.get("host1");
        HostInventory host2 = deployer.hosts.get("host2");

        int spNum = 30;
        for (int i = 0; i < spNum; i++) {
            api.createSnapshot(data.getUuid());
        }

        LocalStorageHostRefVO hcap1 = new LocalStorageHostRefVOFinder().findByPrimaryKey(host1.getUuid(), local.getUuid());
        LocalStorageHostRefVO hcap2 = new LocalStorageHostRefVOFinder().findByPrimaryKey(host2.getUuid(), local.getUuid());

        SimpleQuery<VolumeSnapshotVO> q = dbf.createQuery(VolumeSnapshotVO.class);
        q.add(VolumeSnapshotVO_.volumeUuid, Op.EQ, data.getUuid());
        List<VolumeSnapshotVO> snapshots = q.list();

        config.checkMd5Success = false;
        config.deleteBitsCmds.clear();
        boolean s = false;
        try {
            api.localStorageMigrateVolume(data.getUuid(), host2.getUuid(), null);
        } catch (ApiSenderException e) {
            s = true;
        }
        Assert.assertTrue(s);

        TimeUnit.SECONDS.sleep(2);

        Assert.assertFalse(config.deleteBitsCmds.isEmpty());
        for (final VolumeSnapshotVO sp : snapshots) {
            LocalStorageResourceRefVO spRef = Q.New(LocalStorageResourceRefVO.class)
                    .eq(LocalStorageResourceRefVO_.resourceUuid, sp.getUuid())
                    .find();
            Assert.assertEquals(host1.getUuid(), spRef.getHostUuid());

            DeleteBitsCmd deleteBitsCmd = CollectionUtils.find(config.deleteBitsCmds, new Function<DeleteBitsCmd, DeleteBitsCmd>() {
                @Override
                public DeleteBitsCmd call(DeleteBitsCmd arg) {
                    return arg.getPath().equals(sp.getPrimaryStorageInstallPath()) ? arg : null;
                }
            });
            Assert.assertNotNull(String.format("fails to check snapshot[uuid:%s], deleteBitsCmd", sp.getUuid()), deleteBitsCmd);
            Assert.assertEquals(host2.getUuid(), deleteBitsCmd.getHostUuid());
        }

        LocalStorageResourceRefVO dref = Q.New(LocalStorageResourceRefVO.class)
                .eq(LocalStorageResourceRefVO_.resourceUuid, data.getUuid())
                .find();
        Assert.assertEquals(host1.getUuid(), dref.getHostUuid());

        LocalStorageHostRefVO hcap11 = new LocalStorageHostRefVOFinder().findByPrimaryKey(host1.getUuid(), local.getUuid());
        LocalStorageHostRefVO hcap22 = new LocalStorageHostRefVOFinder().findByPrimaryKey(host2.getUuid(), local.getUuid());
        Assert.assertEquals(hcap1.getAvailableCapacity(), hcap11.getAvailableCapacity());
        Assert.assertEquals(hcap2.getAvailableCapacity(), hcap22.getAvailableCapacity());
    }
}
