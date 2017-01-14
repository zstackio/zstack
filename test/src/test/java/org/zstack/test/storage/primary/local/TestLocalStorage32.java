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
import org.zstack.header.image.ImageInventory;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.storage.primary.PrimaryStorageOverProvisioningManager;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO_;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeType;
import org.zstack.storage.primary.local.*;
import org.zstack.storage.primary.local.LocalStorageKvmBackend.*;
import org.zstack.storage.primary.local.LocalStorageKvmSftpBackupStorageMediatorImpl.SftpDownloadBitsCmd;
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
 * 4. migrate the root volume to host2
 * <p>
 * confirm the migration succeeded
 * <p>
 * 5. attach the data volume
 * <p>
 * confirm unable to attach
 * <p>
 * 6. migrate the data volume to the host2
 * <p>
 * confirm the migration succeeded
 * <p>
 * 7. attach the data volume
 * <p>
 * confirm the attach succeeded
 * <p>
 * 8. take 30 snapshots of the data volume
 * 9. migrate the data volume to the host1
 * <p>
 * confirm the migration of the volume and snapshots succeeded
 */
public class TestLocalStorage32 {
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
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        api.stopVmInstance(vm.getUuid());

        PrimaryStorageInventory local = deployer.primaryStorages.get("local");
        VolumeInventory root = vm.getRootVolume();
        VolumeInventory data = CollectionUtils.find(vm.getAllVolumes(), new Function<VolumeInventory, VolumeInventory>() {
            @Override
            public VolumeInventory call(VolumeInventory arg) {
                return VolumeType.Data.toString().equals(arg.getType()) ? arg : null;
            }
        });

        api.detachVolumeFromVm(data.getUuid());

        ImageInventory image = deployer.images.get("TestImage");

        long rootVolumeSize = psRatioMgr.calculateByRatio(local.getUuid(), root.getSize());
        long dataVolumeSize = psRatioMgr.calculateByRatio(local.getUuid(), data.getSize());
        long imageSize = image.getSize();

        HostInventory host1 = deployer.hosts.get("host1");
        HostInventory host2 = deployer.hosts.get("host2");
        LocalStorageHostRefVO hcap1 = new LocalStorageHostRefVOFinder().findByPrimaryKey(host1.getUuid(), local.getUuid());
        LocalStorageHostRefVO hcap2 = new LocalStorageHostRefVOFinder().findByPrimaryKey(host2.getUuid(), local.getUuid());

        config.downloadBitsCmds.clear();
        LocalStorageResourceRefInventory ref = api.localStorageMigrateVolume(root.getUuid(), host2.getUuid(), null);

        Assert.assertEquals(1, config.downloadBitsCmds.size());
        SftpDownloadBitsCmd downloadBitsCmd = config.downloadBitsCmds.get(0);
        Assert.assertEquals(image.getBackupStorageRefs().get(0).getInstallPath(), downloadBitsCmd.getBackupStorageInstallPath());

        Assert.assertEquals(host2.getUuid(), ref.getHostUuid());
        LocalStorageHostRefVO hcap11 = new LocalStorageHostRefVOFinder().findByPrimaryKey(host1.getUuid(), local.getUuid());
        LocalStorageHostRefVO hcap22 = new LocalStorageHostRefVOFinder().findByPrimaryKey(host2.getUuid(), local.getUuid());
        Assert.assertEquals(hcap1.getAvailableCapacity() + rootVolumeSize, hcap11.getAvailableCapacity());
        Assert.assertEquals(hcap2.getAvailableCapacity() - rootVolumeSize - imageSize, hcap22.getAvailableCapacity());

        Assert.assertFalse(config.getMd5Cmds.isEmpty());
        GetMd5Cmd getMd5Cmd = config.getMd5Cmds.get(0);
        Assert.assertEquals(1, getMd5Cmd.md5s.size());
        Assert.assertEquals(root.getUuid(), getMd5Cmd.md5s.get(0).resourceUuid);
        Assert.assertEquals(root.getInstallPath(), getMd5Cmd.md5s.get(0).path);

        Assert.assertFalse(config.copyBitsFromRemoteCmds.isEmpty());

        Assert.assertFalse(config.checkMd5sumCmds.isEmpty());
        CheckMd5sumCmd checkMd5sumCmd = config.checkMd5sumCmds.get(0);
        Assert.assertEquals(1, checkMd5sumCmd.md5s.size());
        Assert.assertEquals(root.getUuid(), checkMd5sumCmd.md5s.get(0).resourceUuid);
        Assert.assertEquals(root.getInstallPath(), checkMd5sumCmd.md5s.get(0).path);

        TimeUnit.SECONDS.sleep(2);
        Assert.assertFalse(config.deleteBitsCmds.isEmpty());
        DeleteBitsCmd deleteBitsCmd = config.deleteBitsCmds.get(0);
        Assert.assertEquals(host1.getUuid(), deleteBitsCmd.getHostUuid());
        Assert.assertEquals(root.getInstallPath(), deleteBitsCmd.getPath());

        vm = api.startVmInstance(vm.getUuid());
        Assert.assertEquals(host2.getUuid(), vm.getHostUuid());

        boolean s = false;
        try {
            // unable to attach because the data volume and the vm is not on the same host
            api.attachVolumeToVm(vm.getUuid(), data.getUuid());
        } catch (ApiSenderException e) {
            s = true;
        }
        Assert.assertTrue(s);

        ref = api.localStorageMigrateVolume(data.getUuid(), host2.getUuid(), null);
        Assert.assertEquals(host2.getUuid(), ref.getHostUuid());
        LocalStorageHostRefVO hcap111 = new LocalStorageHostRefVOFinder().findByPrimaryKey(host1.getUuid(), local.getUuid());
        LocalStorageHostRefVO hcap222 = new LocalStorageHostRefVOFinder().findByPrimaryKey(host2.getUuid(), local.getUuid());
        Assert.assertEquals(hcap11.getAvailableCapacity() + dataVolumeSize, hcap111.getAvailableCapacity());
        Assert.assertEquals(hcap22.getAvailableCapacity() - dataVolumeSize, hcap222.getAvailableCapacity());

        // attach successfully
        data = api.attachVolumeToVm(vm.getUuid(), data.getUuid());

        int spNum = 30;
        for (int i = 0; i < spNum; i++) {
            api.createSnapshot(data.getUuid());
        }

        api.detachVolumeFromVm(data.getUuid());
        SimpleQuery<VolumeSnapshotVO> q = dbf.createQuery(VolumeSnapshotVO.class);
        q.add(VolumeSnapshotVO_.volumeUuid, Op.EQ, data.getUuid());
        List<VolumeSnapshotVO> snapshots = q.list();
        long spSize = 0;
        for (VolumeSnapshotVO sp : snapshots) {
            spSize += sp.getSize();
        }

        hcap111 = new LocalStorageHostRefVOFinder().findByPrimaryKey(host1.getUuid(), local.getUuid());
        hcap222 = new LocalStorageHostRefVOFinder().findByPrimaryKey(host2.getUuid(), local.getUuid());
        config.checkMd5sumCmds.clear();
        config.getMd5Cmds.clear();
        config.deleteBitsCmds.clear();
        ref = api.localStorageMigrateVolume(data.getUuid(), host1.getUuid(), null);
        Assert.assertEquals(host1.getUuid(), ref.getHostUuid());
        LocalStorageHostRefVO hcap1111 = new LocalStorageHostRefVOFinder().findByPrimaryKey(host1.getUuid(), local.getUuid());
        LocalStorageHostRefVO hcap2222 = new LocalStorageHostRefVOFinder().findByPrimaryKey(host2.getUuid(), local.getUuid());
        Assert.assertEquals(hcap111.getAvailableCapacity() - dataVolumeSize - spSize, hcap1111.getAvailableCapacity());
        Assert.assertEquals(hcap222.getAvailableCapacity() + dataVolumeSize + spSize, hcap2222.getAvailableCapacity());
        Assert.assertFalse(config.checkMd5sumCmds.isEmpty());
        checkMd5sumCmd = config.checkMd5sumCmds.get(0);

        Assert.assertFalse(config.getMd5Cmds.isEmpty());
        getMd5Cmd = config.getMd5Cmds.get(0);

        TimeUnit.SECONDS.sleep(2);
        for (final VolumeSnapshotVO sp : snapshots) {
            s = false;
            for (Md5TO to : checkMd5sumCmd.md5s) {
                if (to.resourceUuid.equals(sp.getUuid())) {
                    Assert.assertEquals(String.format("fails to check snapshot[uuid:%s], checkMd5sumCmd", sp.getUuid()), sp.getPrimaryStorageInstallPath(), to.path);
                    s = true;
                    break;
                }
            }
            Assert.assertTrue(String.format("fails to check snapshot[uuid:%s], checkMd5sumCmd", sp.getUuid()), s);

            s = false;
            for (GetMd5TO to : getMd5Cmd.md5s) {
                if (to.resourceUuid.equals(sp.getUuid())) {
                    Assert.assertEquals(String.format("fails to check snapshot[uuid:%s], getMd5Cmd", sp.getUuid()), sp.getPrimaryStorageInstallPath(), to.path);
                    s = true;
                    break;
                }
            }
            Assert.assertTrue(String.format("fails to check snapshot[uuid:%s], getMd5Cmd", sp.getUuid()), s);

            LocalStorageResourceRefVO spRef = Q.New(LocalStorageResourceRefVO.class)
                    .eq(LocalStorageResourceRefVO_.resourceUuid, sp.getUuid())
                    .find();
            Assert.assertEquals(host1.getUuid(), spRef.getHostUuid());

            deleteBitsCmd = CollectionUtils.find(config.deleteBitsCmds, new Function<DeleteBitsCmd, DeleteBitsCmd>() {
                @Override
                public DeleteBitsCmd call(DeleteBitsCmd arg) {
                    return arg.getPath().equals(sp.getPrimaryStorageInstallPath()) ? arg : null;
                }
            });
            Assert.assertNotNull(String.format("fails to check snapshot[uuid:%s], deleteBitsCmd", sp.getUuid()), deleteBitsCmd);
            Assert.assertEquals(host2.getUuid(), deleteBitsCmd.getHostUuid());
        }
    }
}
