package org.zstack.test.storage.primary.local;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig.Capacity;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.data.SizeUnit;

/**
 * 1. create two vms on different hosts
 * 2. attach a data volume to the vm1
 * 3. detach the data volume then attach to the vm2
 * <p>
 * confirm the data volume cannot attach to the vm2
 * <p>
 * 4. attach the data volume back to the vm1
 * <p>
 * confirm the attaching success
 */
public class TestLocalStorage11 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    LocalStorageSimulatorConfig config;
    long totalSize = SizeUnit.GIGABYTE.toByte(100);

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/localStorage/TestLocalStorage11.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("localStorageSimulator.xml");
        deployer.addSpringConfig("localStorage.xml");
        deployer.load();

        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(LocalStorageSimulatorConfig.class);

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
    public void test() throws ApiSenderException {
        PrimaryStorageInventory local = deployer.primaryStorages.get("local");
        VmInstanceInventory vm1 = deployer.vms.get("TestVm");
        VmInstanceInventory vm2 = deployer.vms.get("TestVm1");
        DiskOfferingInventory dof = deployer.diskOfferings.get("TestDiskOffering1");
        VolumeInventory data = api.createDataVolume("data", dof.getUuid());
        data = api.attachVolumeToVm(vm1.getUuid(), data.getUuid());
        data = api.detachVolumeFromVm(data.getUuid());

        boolean success = false;
        try {
            api.attachVolumeToVm(vm2.getUuid(), data.getUuid());
        } catch (ApiSenderException e) {
            success = true;
        }

        Assert.assertTrue(success);

        api.attachVolumeToVm(vm1.getUuid(), data.getUuid());
    }
}
