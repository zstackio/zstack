package org.zstack.test.storage.primary.local;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.MigrateVmOnHypervisorMsg.StorageMigrationPolicy;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.storage.primary.ImageCacheVO;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.storage.primary.PrimaryStorageOverProvisioningManager;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO_;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeType;
import org.zstack.header.volume.VolumeVO;
import org.zstack.kvm.KVMAgentCommands.MigrateVmCmd;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.storage.primary.local.*;
import org.zstack.storage.primary.local.LocalStorageKvmBackend.CreateEmptyVolumeCmd;
import org.zstack.storage.primary.local.LocalStorageKvmBackend.DeleteBitsCmd;
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 1. migrate a vm with snapshots
 * <p>
 * confirm all related commands sent
 * confirm the volumes are referenced on the dst host
 * confirm the capacity of the src host and dst host are correct
 */
public class TestLocalStorage43 {
    CLogger logger = Utils.getLogger(TestLocalStorage43.class);
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
        VolumeInventory data = CollectionUtils.find(vm.getAllVolumes(), new Function<VolumeInventory, VolumeInventory>() {
            @Override
            public VolumeInventory call(VolumeInventory arg) {
                return VolumeType.Data.toString().equals(arg.getType()) ? arg : null;
            }
        });

        ImageCacheVO cacheVO = dbf.listAll(ImageCacheVO.class).get(0);
        long imageSize = cacheVO.getSize();
        long usedSize = 0;
        for (VolumeVO vol : dbf.listAll(VolumeVO.class)) {
            usedSize += ratioMgr.calculateByRatio(vol.getPrimaryStorageUuid(), vol.getSize());
        }

        int snum = 10;
        List<VolumeSnapshotInventory> sps = new ArrayList<>();
        for (int i = 0; i < snum; i++) {
            sps.add(api.createSnapshot(vm.getRootVolumeUuid()));
        }

        for (VolumeSnapshotInventory sp : sps) {
            usedSize += sp.getSize();
        }

        SimpleQuery<VolumeSnapshotVO> q = dbf.createQuery(VolumeSnapshotVO.class);
        q.add(VolumeSnapshotVO_.latest, SimpleQuery.Op.EQ, true);
        VolumeSnapshotVO lastestSp = q.find();

        config.createEmptyVolumeCmds.clear();
        config.deleteBitsCmds.clear();
        vm = api.migrateVmInstance(vm.getUuid(), host2.getUuid());
        TimeUnit.SECONDS.sleep(5);

        // confirm capacity are correct no both hosts
        LocalStorageHostRefVO ref1 = new LocalStorageHostRefVOFinder().findByPrimaryKey(host1.getUuid(), local.getUuid());
        Assert.assertEquals(ref1.getTotalCapacity() - imageSize, ref1.getAvailableCapacity());
        LocalStorageHostRefVO ref2 = new LocalStorageHostRefVOFinder().findByPrimaryKey(host2.getUuid(), local.getUuid());
        Assert.assertEquals(ref2.getTotalCapacity() - imageSize - usedSize, ref2.getAvailableCapacity());

        Assert.assertEquals(vm.getAllVolumes().size(), config.createEmptyVolumeCmds.size());
        for (final VolumeInventory vol : vm.getAllVolumes()) {
            // volumes are created on dst host
            CreateEmptyVolumeCmd cmd = CollectionUtils.find(config.createEmptyVolumeCmds, new Function<CreateEmptyVolumeCmd, CreateEmptyVolumeCmd>() {
                @Override
                public CreateEmptyVolumeCmd call(CreateEmptyVolumeCmd arg) {
                    return arg.getVolumeUuid().equals(vol.getUuid()) ? arg : null;
                }
            });
            Assert.assertNotNull(cmd);
            Assert.assertEquals(vol.getInstallPath(), cmd.getInstallUrl());

            if (VolumeType.Root.toString().equals(vol.getType())) {
                Assert.assertEquals(lastestSp.getPrimaryStorageInstallPath(), cmd.getBackingFile());
            }

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

        Assert.assertEquals(1, config.getMd5Cmds.size());
        LocalStorageKvmBackend.GetMd5Cmd getMd5Cmd = config.getMd5Cmds.get(0);
        goOn0:
        for (VolumeSnapshotInventory sp : sps) {
            for (LocalStorageKvmBackend.GetMd5TO to : getMd5Cmd.md5s) {
                if (to.path.equals(sp.getPrimaryStorageInstallPath())) {
                    break goOn0;
                }
            }

            Assert.fail(sp.getUuid());
        }

        Assert.assertEquals(1, config.copyBitsFromRemoteCmds.size());
        LocalStorageKvmMigrateVmFlow.CopyBitsFromRemoteCmd copyBitsFromRemoteCmd = config.copyBitsFromRemoteCmds.get(0);
        for (VolumeSnapshotInventory sp : sps) {
            Assert.assertTrue(sp.getPrimaryStorageInstallPath(), copyBitsFromRemoteCmd.paths.contains(sp.getPrimaryStorageInstallPath()));
        }

        Assert.assertEquals(1, config.checkMd5sumCmds.size());
        LocalStorageKvmBackend.CheckMd5sumCmd checkMd5sumCmd = config.checkMd5sumCmds.get(0);
        goOn:
        for (VolumeSnapshotInventory sp : sps) {
            for (LocalStorageKvmBackend.Md5TO to : checkMd5sumCmd.md5s) {
                if (to.path.equals(sp.getPrimaryStorageInstallPath())) {
                    break goOn;
                }
            }

            Assert.fail(sp.getUuid());
        }

        Assert.assertEquals(1, config.rebaseSnapshotBackingFilesCmds.size());
        LocalStorageKvmMigrateVmFlow.RebaseSnapshotBackingFilesCmd rebaseSnapshotBackingFilesCmd = config.rebaseSnapshotBackingFilesCmds.get(0);
        goOn1:
        for (VolumeSnapshotInventory sp : sps) {
            for (LocalStorageKvmMigrateVmFlow.SnapshotTO to : rebaseSnapshotBackingFilesCmd.snapshots) {
                if (to.path.equals(sp.getPrimaryStorageInstallPath())) {
                    if (sp.getParentUuid() != null) {
                        VolumeSnapshotVO p = dbf.findByUuid(sp.getParentUuid(), VolumeSnapshotVO.class);
                        Assert.assertEquals(to.parentPath, p.getPrimaryStorageInstallPath());
                    }

                    break goOn1;
                }
            }

            Assert.fail(sp.getUuid());
        }

        // verify on both src
        Assert.assertEquals(2, config.verifySnapshotChainCmds.size());
        LocalStorageKvmMigrateVmFlow.VerifySnapshotChainCmd verifySnapshotChainCmd = config.verifySnapshotChainCmds.get(0);
        goOn2:
        for (VolumeSnapshotInventory sp : sps) {
            for (LocalStorageKvmMigrateVmFlow.SnapshotTO to : verifySnapshotChainCmd.snapshots) {
                if (to.path.equals(sp.getPrimaryStorageInstallPath())) {
                    if (sp.getParentUuid() != null) {
                        VolumeSnapshotVO p = dbf.findByUuid(sp.getParentUuid(), VolumeSnapshotVO.class);
                        Assert.assertEquals(to.parentPath, p.getPrimaryStorageInstallPath());
                    }

                    break goOn2;
                }
            }

            Assert.fail(sp.getUuid());
        }

        // verify on dst
        verifySnapshotChainCmd = config.verifySnapshotChainCmds.get(1);
        goOn5:
        for (VolumeSnapshotInventory sp : sps) {
            for (LocalStorageKvmMigrateVmFlow.SnapshotTO to : verifySnapshotChainCmd.snapshots) {
                if (to.path.equals(sp.getPrimaryStorageInstallPath())) {
                    if (sp.getParentUuid() != null) {
                        VolumeSnapshotVO p = dbf.findByUuid(sp.getParentUuid(), VolumeSnapshotVO.class);
                        Assert.assertEquals(to.parentPath, p.getPrimaryStorageInstallPath());
                    }

                    break goOn5;
                }
            }

            Assert.fail(sp.getUuid());
        }

        boolean s = false;
        for (LocalStorageKvmMigrateVmFlow.SnapshotTO to : verifySnapshotChainCmd.snapshots) {
            if (to.path.equals(vm.getRootVolume().getInstallPath()) && to.parentPath.equals(lastestSp.getPrimaryStorageInstallPath())) {
                s = true;
                break;
            }
        }
        Assert.assertTrue(s);

        // deleted src volume and snapshots
        Assert.assertEquals(snum + vm.getAllVolumes().size(), config.deleteBitsCmds.size());
        goOn3:
        for (VolumeSnapshotInventory sp : sps) {
            for (DeleteBitsCmd dcmd : config.deleteBitsCmds) {
                if (sp.getPrimaryStorageInstallPath().equals(dcmd.getPath())) {
                    break goOn3;
                }
            }

            Assert.fail(sp.getUuid());
        }

        // root volume deleted
        s = false;
        for (DeleteBitsCmd dcmd : config.deleteBitsCmds) {
            if (vm.getRootVolume().getInstallPath().equals(dcmd.getPath())) {
                s = true;
                break;
            }
        }
        Assert.assertTrue(s);

        // data volume deleted
        s = false;
        for (DeleteBitsCmd dcmd : config.deleteBitsCmds) {
            if (data.getInstallPath().equals(dcmd.getPath())) {
                s = true;
                break;
            }
        }
        Assert.assertTrue(s);
    }
}
