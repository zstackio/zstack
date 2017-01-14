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
import org.zstack.header.host.MigrateVmOnHypervisorMsg.StorageMigrationPolicy;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.storage.primary.ImageCacheVO;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.storage.primary.PrimaryStorageOverProvisioningManager;
import org.zstack.header.storage.snapshot.VolumeSnapshotTreeVO;
import org.zstack.header.storage.snapshot.VolumeSnapshotTreeVO_;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO_;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeVO;
import org.zstack.kvm.KVMAgentCommands.MigrateVmCmd;
import org.zstack.kvm.KVMHostVO;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.storage.primary.local.*;
import org.zstack.storage.primary.local.LocalStorageKvmBackend.CreateEmptyVolumeCmd;
import org.zstack.storage.primary.local.LocalStorageKvmBackend.DeleteBitsCmd;
import org.zstack.storage.primary.local.LocalStorageKvmMigrateVmFlow.CopyBitsFromRemoteCmd;
import org.zstack.storage.primary.local.LocalStorageKvmMigrateVmFlow.RebaseSnapshotBackingFilesCmd;
import org.zstack.storage.primary.local.LocalStorageKvmMigrateVmFlow.SnapshotTO;
import org.zstack.storage.primary.local.LocalStorageKvmMigrateVmFlow.VerifySnapshotChainCmd;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig.Capacity;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 1. migrate a vm with storage
 * <p>
 * confirm all related commands sent
 * confirm the volumes are referenced on the dst host
 * confirm the capacity of the src host and dst host are correct
 * <p>
 * 2. get migration target host
 * <p>
 * confirm only host1 is the target
 * <p>
 * 3. migrate the vm to a simulator host
 * <p>
 * confirm the migration failed
 * <p>
 * 4. put the host2 to the maintenance mode
 * <p>
 * confirm the vm stopped
 */
public class TestLocalStorage31 {
    CLogger logger = Utils.getLogger(TestLocalStorage31.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    KVMSimulatorConfig kconfig;
    LocalStorageSimulatorConfig config;
    PrimaryStorageOverProvisioningManager ratioMgr;
    long totalSize = SizeUnit.GIGABYTE.toByte(100);

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/localStorage/TestLocalStorage28.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("localStorageSimulator.xml");
        deployer.addSpringConfig("localStorage.xml");
        deployer.load();

        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(LocalStorageSimulatorConfig.class);
        kconfig = loader.getComponent(KVMSimulatorConfig.class);
        ratioMgr = loader.getComponent(PrimaryStorageOverProvisioningManager.class);

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
        HostInventory host2 = deployer.hosts.get("host2");
        HostInventory host1 = deployer.hosts.get("host1");
        VmInstanceInventory vm = deployer.vms.get("TestVm");

        int spNum = 25;
        for (int i = 0; i < spNum; i++) {
            api.createSnapshot(vm.getRootVolume().getUuid());
        }

        ImageCacheVO cacheVO = dbf.listAll(ImageCacheVO.class).get(0);
        long imageSize = cacheVO.getSize();
        long usedVolumeSize = 0;
        for (VolumeVO vol : dbf.listAll(VolumeVO.class)) {
            usedVolumeSize += ratioMgr.calculateByRatio(vol.getPrimaryStorageUuid(), vol.getSize());
        }
        SimpleQuery<VolumeSnapshotVO> q = dbf.createQuery(VolumeSnapshotVO.class);
        q.add(VolumeSnapshotVO_.volumeUuid, Op.EQ, vm.getRootVolume().getUuid());
        List<VolumeSnapshotVO> snapshthots = q.list();

        for (VolumeSnapshotVO sp : snapshthots) {
            usedVolumeSize += sp.getSize();
        }

        LocalStorageHostRefVO ref1 = new LocalStorageHostRefVOFinder().findByPrimaryKey(host1.getUuid(), local.getUuid());
        Assert.assertEquals(ref1.getTotalCapacity() - imageSize - usedVolumeSize, ref1.getAvailableCapacity());

        config.createEmptyVolumeCmds.clear();
        config.deleteBitsCmds.clear();
        vm = api.migrateVmInstance(vm.getUuid(), host2.getUuid());
        TimeUnit.SECONDS.sleep(5);
        final VolumeVO root = dbf.findByUuid(vm.getRootVolume().getUuid(), VolumeVO.class);

        Assert.assertEquals(vm.getAllVolumes().size(), config.createEmptyVolumeCmds.size());
        Assert.assertEquals(2, config.verifySnapshotChainCmds.size());
        Assert.assertEquals(1, config.rebaseSnapshotBackingFilesCmds.size());
        Assert.assertEquals(1, config.copyBitsFromRemoteCmds.size());
        VerifySnapshotChainCmd vcmd = config.verifySnapshotChainCmds.get(0);
        RebaseSnapshotBackingFilesCmd rcmd = config.rebaseSnapshotBackingFilesCmds.get(0);
        CopyBitsFromRemoteCmd ccmd = config.copyBitsFromRemoteCmds.get(0);

        KVMHostVO kvm = dbf.findByUuid(host2.getUuid(), KVMHostVO.class);
        Assert.assertEquals(kvm.getManagementIp(), ccmd.dstIp);
        Assert.assertEquals(kvm.getUsername(), ccmd.dstUsername);
        Assert.assertEquals(kvm.getPassword(), ccmd.dstPassword);

        for (final VolumeSnapshotVO sp : snapshthots) {
            // snapshots are copied on dst host
            String copyPath = CollectionUtils.find(ccmd.paths, new Function<String, String>() {
                @Override
                public String call(String arg) {
                    return arg.equals(sp.getPrimaryStorageInstallPath()) ? arg : null;
                }
            });
            Assert.assertNotNull(copyPath);

            // snapshots are verified
            SnapshotTO to = CollectionUtils.find(vcmd.snapshots, new Function<SnapshotTO, SnapshotTO>() {
                @Override
                public SnapshotTO call(SnapshotTO arg) {
                    return arg.snapshotUuid.equals(sp.getUuid()) ? arg : null;
                }
            });
            Assert.assertNotNull(to);
            Assert.assertEquals(sp.getPrimaryStorageInstallPath(), to.path);
            if (sp.getParentUuid() != null) {
                VolumeSnapshotVO p = dbf.findByUuid(sp.getParentUuid(), VolumeSnapshotVO.class);
                Assert.assertNotNull(p);
                Assert.assertEquals(p.getPrimaryStorageInstallPath(), to.parentPath);
            }

            // the reference to snapshots are changed to the host2
            LocalStorageResourceRefVO ref = Q.New(LocalStorageResourceRefVO.class)
                    .eq(LocalStorageResourceRefVO_.resourceUuid, sp.getUuid())
                    .find();
            Assert.assertEquals(ref.getHostUuid(), host2.getUuid());

            // snapshots are rebased
            to = CollectionUtils.find(rcmd.snapshots, new Function<SnapshotTO, SnapshotTO>() {
                @Override
                public SnapshotTO call(SnapshotTO arg) {
                    return arg.snapshotUuid.equals(sp.getUuid()) ? arg : null;
                }
            });
            Assert.assertNotNull(to);
            Assert.assertEquals(sp.getPrimaryStorageInstallPath(), to.path);
            if (sp.getParentUuid() != null) {
                VolumeSnapshotVO p = dbf.findByUuid(sp.getParentUuid(), VolumeSnapshotVO.class);
                Assert.assertNotNull(p);
                Assert.assertEquals(p.getPrimaryStorageInstallPath(), to.parentPath);
            }

            // snapshot are deleted from the src host
            DeleteBitsCmd dcmd = CollectionUtils.find(config.deleteBitsCmds, new Function<DeleteBitsCmd, DeleteBitsCmd>() {
                @Override
                public DeleteBitsCmd call(DeleteBitsCmd arg) {
                    return arg.getPath().equals(sp.getPrimaryStorageInstallPath()) ? arg : null;
                }
            });
            Assert.assertNotNull(String.format("snapshot[%s] not deleted", sp.getPrimaryStorageInstallPath()), dcmd);
        }

        List<CreateEmptyVolumeCmd> cmds = CollectionUtils.transformToList(config.createEmptyVolumeCmds, new Function<CreateEmptyVolumeCmd, CreateEmptyVolumeCmd>() {
            @Override
            public CreateEmptyVolumeCmd call(CreateEmptyVolumeCmd arg) {
                return arg.getInstallUrl().equals(root.getInstallPath()) ? arg : null;
            }
        });
        Assert.assertEquals(1, cmds.size());

        // the link to the parent of the root volume is verified
        SnapshotTO to = CollectionUtils.find(vcmd.snapshots, new Function<SnapshotTO, SnapshotTO>() {
            @Override
            public SnapshotTO call(SnapshotTO arg) {
                return arg.snapshotUuid.equals(root.getUuid()) ? arg : null;
            }
        });
        Assert.assertNotNull(to);
        Assert.assertEquals(root.getInstallPath(), to.path);

        SimpleQuery<VolumeSnapshotTreeVO> sq = dbf.createQuery(VolumeSnapshotTreeVO.class);
        sq.add(VolumeSnapshotTreeVO_.volumeUuid, Op.EQ, root.getUuid());
        sq.add(VolumeSnapshotTreeVO_.current, Op.EQ, true);
        VolumeSnapshotTreeVO currentTree = sq.find();

        SimpleQuery<VolumeSnapshotVO> spq = dbf.createQuery(VolumeSnapshotVO.class);
        spq.add(VolumeSnapshotVO_.treeUuid, Op.EQ, currentTree.getUuid());
        spq.add(VolumeSnapshotVO_.latest, Op.EQ, true);
        VolumeSnapshotVO latest = spq.find();
        Assert.assertEquals(latest.getPrimaryStorageInstallPath(), to.parentPath);

        // the root volume is rebased on the dst host
        to = CollectionUtils.find(rcmd.snapshots, new Function<SnapshotTO, SnapshotTO>() {
            @Override
            public SnapshotTO call(SnapshotTO arg) {
                return arg.snapshotUuid.equals(root.getUuid()) ? arg : null;
            }
        });
        Assert.assertNotNull(to);
        Assert.assertEquals(root.getInstallPath(), to.path);
        Assert.assertEquals(latest.getPrimaryStorageInstallPath(), to.parentPath);

        ref1 = new LocalStorageHostRefVOFinder().findByPrimaryKey(host1.getUuid(), local.getUuid());
        Assert.assertEquals(ref1.getTotalCapacity() - imageSize, ref1.getAvailableCapacity());
        LocalStorageHostRefVO ref2 = new LocalStorageHostRefVOFinder().findByPrimaryKey(host2.getUuid(), local.getUuid());
        Assert.assertEquals(ref2.getTotalCapacity() - imageSize - usedVolumeSize, ref2.getAvailableCapacity());

        for (final VolumeInventory vol : vm.getAllVolumes()) {
            // volumes are created on dst host
            CreateEmptyVolumeCmd createEmptyVolumeCmd = CollectionUtils.find(config.createEmptyVolumeCmds, new Function<CreateEmptyVolumeCmd, CreateEmptyVolumeCmd>() {
                @Override
                public CreateEmptyVolumeCmd call(CreateEmptyVolumeCmd arg) {
                    return arg.getInstallUrl().equals(vol.getInstallPath()) ? arg : null;
                }
            });
            Assert.assertNotNull(createEmptyVolumeCmd);
            Assert.assertEquals(vol.getUuid(), createEmptyVolumeCmd.getVolumeUuid());
            Assert.assertEquals(vol.getSize(), createEmptyVolumeCmd.getSize());

            LocalStorageResourceRefVO r = Q.New(LocalStorageResourceRefVO.class)
                    .eq(LocalStorageResourceRefVO_.resourceUuid, vol.getUuid())
                    .find();
            Assert.assertEquals(host2.getUuid(), r.getHostUuid());

            // volumes are deleted on src host
            DeleteBitsCmd dcmd = CollectionUtils.find(config.deleteBitsCmds, new Function<DeleteBitsCmd, DeleteBitsCmd>() {
                @Override
                public DeleteBitsCmd call(DeleteBitsCmd arg) {
                    return arg.getPath().equals(vol.getInstallPath()) ? arg : null;
                }
            });
            Assert.assertNotNull(String.format("no delete command for volume[uuid:%s, path:%s]", vol.getUuid(), vol.getInstallPath()), dcmd);
        }

        Assert.assertFalse(kconfig.migrateVmCmds.isEmpty());
        MigrateVmCmd mcmd = kconfig.migrateVmCmds.get(0);
        Assert.assertEquals(host2.getManagementIp(), mcmd.getDestHostIp());
        Assert.assertEquals(vm.getUuid(), mcmd.getVmUuid());
        Assert.assertEquals(StorageMigrationPolicy.IncCopy.toString(), mcmd.getStorageMigrationPolicy());
    }
}
