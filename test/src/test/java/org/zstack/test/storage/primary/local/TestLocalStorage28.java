package org.zstack.test.storage.primary.local;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.MigrateVmOnHypervisorMsg.StorageMigrationPolicy;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.message.AbstractBeforeDeliveryMessageInterceptor;
import org.zstack.header.message.Message;
import org.zstack.header.storage.primary.DownloadImageToPrimaryStorageCacheMsg;
import org.zstack.header.storage.primary.ImageCacheVO;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.storage.primary.PrimaryStorageOverProvisioningManager;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeType;
import org.zstack.header.volume.VolumeVO;
import org.zstack.kvm.KVMAgentCommands.MigrateVmCmd;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.storage.primary.local.*;
import org.zstack.storage.primary.local.LocalStorageKvmBackend.CacheInstallPath;
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
public class TestLocalStorage28 {
    CLogger logger = Utils.getLogger(TestLocalStorage28.class);
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
    boolean downloadImageCalled = false;

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

        ImageCacheVO cacheVO = dbf.listAll(ImageCacheVO.class).get(0);
        long imageSize = cacheVO.getSize();
        long usedVolumeSize = 0;
        for (VolumeVO vol : dbf.listAll(VolumeVO.class)) {
            usedVolumeSize += ratioMgr.calculateByRatio(vol.getPrimaryStorageUuid(), vol.getSize());
        }


        CacheInstallPath cp = new CacheInstallPath();
        cp.fullPath = cacheVO.getInstallUrl();
        cp.disassemble();

        bus.installBeforeDeliveryMessageInterceptor(new AbstractBeforeDeliveryMessageInterceptor() {
            @Override
            public void intercept(Message msg) {
                if (msg instanceof DownloadImageToPrimaryStorageCacheMsg) {
                    downloadImageCalled = true;
                }
            }
        }, DownloadImageToPrimaryStorageCacheMsg.class);

        config.createEmptyVolumeCmds.clear();
        config.deleteBitsCmds.clear();
        vm = api.migrateVmInstance(vm.getUuid(), host2.getUuid());
        TimeUnit.SECONDS.sleep(5);

        Assert.assertTrue(downloadImageCalled);

        LocalStorageHostRefVO ref1 = new LocalStorageHostRefVOFinder().findByPrimaryKey(host1.getUuid(), local.getUuid());
        Assert.assertEquals(ref1.getTotalCapacity() - imageSize, ref1.getAvailableCapacity());
        LocalStorageHostRefVO ref2 = new LocalStorageHostRefVOFinder().findByPrimaryKey(host2.getUuid(), local.getUuid());
        Assert.assertEquals(ref2.getTotalCapacity() - imageSize - usedVolumeSize, ref2.getAvailableCapacity());

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
                Assert.assertEquals(cp.installPath, cmd.getBackingFile());
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

        List<HostInventory> hosts = api.getMigrationTargetHost(vm.getUuid());
        Assert.assertFalse(hosts.isEmpty());
        HostInventory thost = hosts.get(0);
        Assert.assertEquals(host1.getUuid(), thost.getUuid());

        HostInventory simHost = deployer.hosts.get("TestHost1");
        boolean s = false;
        try {
            api.migrateVmInstance(vm.getUuid(), simHost.getUuid());
        } catch (ApiSenderException e) {
            s = true;
        }
        Assert.assertTrue(s);

        api.maintainHost(host2.getUuid());
        VmInstanceVO vmvo = dbf.findByUuid(vm.getUuid(), VmInstanceVO.class);
        Assert.assertEquals(VmInstanceState.Stopped, vmvo.getState());
    }
}
