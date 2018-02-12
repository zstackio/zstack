package org.zstack.test.storage.primary.local;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.host.HostInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.message.AbstractBeforeDeliveryMessageInterceptor;
import org.zstack.header.message.Message;
import org.zstack.header.storage.primary.ImageCacheVO;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.storage.primary.PrimaryStorageOverProvisioningManager;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeVO;
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

import java.util.concurrent.TimeUnit;

/**
 * 1. create a vm with local storage
 * 2. set migration to fail
 * 3. migrate the vm
 * <p>
 * confirm the migration failed
 * confirm all resources are returned
 */
public class TestLocalStorage29 {
    CLogger logger = Utils.getLogger(TestLocalStorage29.class);
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
    boolean directDeleteOnDst = false;
    boolean directDeleteOnSrc = false;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/localStorage/TestLocalStorage4.xml", con);
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
        final HostInventory host2 = deployer.hosts.get("host2");
        final HostInventory host1 = deployer.hosts.get("host1");
        VmInstanceInventory vm = deployer.vms.get("TestVm");

        ImageCacheVO cacheVO = dbf.listAll(ImageCacheVO.class).get(0);
        long imageSize = cacheVO.getSize();
        long usedVolumeSize = 0;
        for (VolumeVO vol : dbf.listAll(VolumeVO.class)) {
            usedVolumeSize += ratioMgr.calculateByRatio(vol.getPrimaryStorageUuid(), vol.getSize());
        }

        bus.installBeforeDeliveryMessageInterceptor(new AbstractBeforeDeliveryMessageInterceptor() {
            @Override
            public void intercept(Message msg) {
                // when rollback, the volumes are deleted on the dst host
                LocalStorageDirectlyDeleteBitsMsg lmsg = (LocalStorageDirectlyDeleteBitsMsg) msg;
                if (lmsg.getHostUuid().equals(host2.getUuid())) {
                    directDeleteOnDst = true;
                }

                // if the deleting happens on the src host, something goes wrong
                if (lmsg.getHostUuid().equals(host1.getUuid())) {
                    directDeleteOnSrc = true;
                }
            }
        }, LocalStorageDirectlyDeleteBitsMsg.class);

        config.createEmptyVolumeCmds.clear();
        config.deleteBitsCmds.clear();
        kconfig.migrateVmSuccess = false;
        boolean s = false;
        try {
            vm = api.migrateVmInstance(vm.getUuid(), host2.getUuid());
        } catch (ApiSenderException e) {
            s = true;
        }
        Assert.assertTrue(s);
        TimeUnit.SECONDS.sleep(3);

        Assert.assertTrue(directDeleteOnDst);
        Assert.assertFalse(directDeleteOnSrc);
        LocalStorageHostRefVO ref1 = new LocalStorageHostRefVOFinder().findByPrimaryKey(host1.getUuid(), local.getUuid());
        Assert.assertEquals(ref1.getTotalCapacity() - imageSize - usedVolumeSize, ref1.getAvailableCapacity());
        // even migration failed, the image is always downloaded to the image cache, which will not be rolled back
        LocalStorageHostRefVO ref2 = new LocalStorageHostRefVOFinder().findByPrimaryKey(host2.getUuid(), local.getUuid());
        Assert.assertEquals(ref2.getTotalCapacity() - imageSize, ref2.getAvailableCapacity());

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

            // volumes are deleted on dst host because of rollback
            DeleteBitsCmd dcmd = CollectionUtils.find(config.deleteBitsCmds, new Function<DeleteBitsCmd, DeleteBitsCmd>() {
                @Override
                public DeleteBitsCmd call(DeleteBitsCmd arg) {
                    return arg.getPath().equals(vol.getInstallPath()) ? arg : null;
                }
            });
            Assert.assertNotNull(dcmd);

            // volumes are still on the src host
            LocalStorageResourceRefVO r = Q.New(LocalStorageResourceRefVO.class)
                    .eq(LocalStorageResourceRefVO_.resourceUuid, vol.getUuid())
                    .find();
            Assert.assertEquals(host1.getUuid(), r.getHostUuid());
        }
    }
}
