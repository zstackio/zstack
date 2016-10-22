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

import java.util.List;

/**
 * 1. use local storage and nfs storage
 * 2. create a two vms: vm1 and vm2 on different hosts
 * 3. attach a data volume to the vm1
 * 4. detach the data volume from the vm1
 * 5. attach the data volume to the vm2
 * <p>
 * confirm the attaching success
 */
public class TestLocalStorage12 {
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
        deployer = new Deployer("deployerXml/localStorage/TestLocalStorage12.xml", con);
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
        PrimaryStorageInventory local2 = deployer.primaryStorages.get("local2");
        PrimaryStorageInventory nfs = deployer.primaryStorages.get("nfs");
        VmInstanceInventory vm1 = deployer.vms.get("TestVm");
        VmInstanceInventory vm2 = deployer.vms.get("TestVm1");
        DiskOfferingInventory dof = deployer.diskOfferings.get("TestDiskOffering1");
        VolumeInventory dataVolume = api.createDataVolume("data", dof.getUuid());

        dataVolume = api.attachVolumeToVm(vm1.getUuid(), dataVolume.getUuid());
        dataVolume = api.detachVolumeFromVm(dataVolume.getUuid());

        List<VolumeInventory> vols = api.getVmAttachableVolume(vm2.getUuid());

        Assert.assertEquals(1, vols.size());

        api.attachVolumeToVm(vm2.getUuid(), dataVolume.getUuid());
    }
}
