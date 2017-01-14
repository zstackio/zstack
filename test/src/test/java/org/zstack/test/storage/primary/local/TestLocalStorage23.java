package org.zstack.test.storage.primary.local;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.storage.primary.local.LocalStorageResourceRefVO;
import org.zstack.storage.primary.local.LocalStorageResourceRefVO_;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig.Capacity;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.data.SizeUnit;

/**
 * 1. use local storage
 * 2. create a vm
 * 3. stop the vm
 * 4. take a snapshot from vm's root volume
 * 5. create a volume from the snapshot
 * <p>
 * confirm the volume created successfully
 * <p>
 * 6. attach the volume to the vm
 * <p>
 * confirm the volume attached successfully
 */
public class TestLocalStorage23 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    LocalStorageSimulatorConfig config;
    KVMSimulatorConfig kconfig;
    long totalSize = SizeUnit.GIGABYTE.toByte(100);

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/localStorage/TestLocalStorage1.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("localStorageSimulator.xml");
        deployer.addSpringConfig("localStorage.xml");
        deployer.load();

        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(LocalStorageSimulatorConfig.class);
        kconfig = loader.getComponent(KVMSimulatorConfig.class);

        Capacity c = new Capacity();
        c.total = totalSize;
        c.avail = totalSize;

        config.capacityMap.put("host1", c);

        deployer.build();
        api = deployer.getApi();
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException {
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        api.stopVmInstance(vm.getUuid());

        VolumeSnapshotInventory sp = api.createSnapshot(vm.getRootVolumeUuid());
        Assert.assertFalse(kconfig.snapshotCmds.isEmpty());
        LocalStorageResourceRefVO ref = Q.New(LocalStorageResourceRefVO.class)
                .eq(LocalStorageResourceRefVO_.resourceUuid, sp.getUuid())
                .find();
        Assert.assertNotNull(ref);
        Assert.assertEquals(vm.getHostUuid(), ref.getHostUuid());

        sp = api.createSnapshot(vm.getRootVolumeUuid());
        ref = Q.New(LocalStorageResourceRefVO.class)
                .eq(LocalStorageResourceRefVO_.resourceUuid, sp.getUuid())
                .find();
        Assert.assertNotNull(ref);
        Assert.assertEquals(vm.getHostUuid(), ref.getHostUuid());

        sp = api.createSnapshot(vm.getRootVolumeUuid());
        ref = Q.New(LocalStorageResourceRefVO.class)
                .eq(LocalStorageResourceRefVO_.resourceUuid, sp.getUuid())
                .find();
        Assert.assertNotNull(ref);
        Assert.assertEquals(vm.getHostUuid(), ref.getHostUuid());

        VolumeInventory data = api.createDataVolumeFromSnapshot(sp.getUuid());

        Assert.assertFalse(config.mergeSnapshotCmds.isEmpty());

        api.attachVolumeToVm(vm.getUuid(), data.getUuid());
    }
}
