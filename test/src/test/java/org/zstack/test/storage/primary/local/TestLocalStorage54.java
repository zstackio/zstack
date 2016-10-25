package org.zstack.test.storage.primary.local;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.thread.AsyncThread;
import org.zstack.header.host.HostInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.storage.primary.PrimaryStorageOverProvisioningManager;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.storage.primary.local.APILocalStorageMigrateVolumeEvent;
import org.zstack.storage.primary.local.APILocalStorageMigrateVolumeMsg;
import org.zstack.storage.primary.local.LocalStorageKvmBackend.DeleteBitsCmd;
import org.zstack.storage.primary.local.LocalStorageKvmMigrateVmFlow.CopyBitsFromRemoteCmd;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig.Capacity;
import org.zstack.test.*;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.data.SizeUnit;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 1. local storage
 * 2. migrate a root volume twice to the same host
 * <p>
 * confirm the volume only deleted on the src host
 * <p>
 * for bug https://github.com/zxwing/premium/issues/633
 */
public class TestLocalStorage54 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    LocalStorageSimulatorConfig config;
    PrimaryStorageOverProvisioningManager psRatioMgr;
    long totalSize = SizeUnit.GIGABYTE.toByte(100);
    CountDownLatch latch = new CountDownLatch(2);

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/localStorage/TestLocalStorage54.xml", con);
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

    @AsyncThread
    void migrateVolume(VolumeInventory vol, String destHost) throws ApiSenderException {
        try {
            APILocalStorageMigrateVolumeMsg msg = new APILocalStorageMigrateVolumeMsg();
            msg.setVolumeUuid(vol.getUuid());
            msg.setDestHostUuid(destHost);
            msg.setSession(api.getAdminSession());
            ApiSender sender = new ApiSender();
            sender.send(msg, APILocalStorageMigrateVolumeEvent.class);
        } finally {
            latch.countDown();
        }
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        api.stopVmInstance(vm.getUuid());

        config.deleteBitsCmds.clear();
        HostInventory host2 = deployer.hosts.get("host2");
        HostInventory host1 = deployer.hosts.get("host1");
        VolumeInventory root = vm.getRootVolume();
        // xml file defined vm was on host1
        migrateVolume(root, host2.getUuid());
        migrateVolume(root, host2.getUuid());
        latch.await(1, TimeUnit.MINUTES);

        Assert.assertEquals(1, config.deleteBitsCmds.size());
        DeleteBitsCmd cmd = config.deleteBitsCmds.get(0);
        Assert.assertEquals(host1.getUuid(), cmd.getHostUuid());
        Assert.assertEquals(1, config.copyBitsFromRemoteCmds.size());
        CopyBitsFromRemoteCmd ccmd = config.copyBitsFromRemoteCmds.get(0);
        Assert.assertEquals(host2.getManagementIp(), ccmd.dstIp);
    }
}
