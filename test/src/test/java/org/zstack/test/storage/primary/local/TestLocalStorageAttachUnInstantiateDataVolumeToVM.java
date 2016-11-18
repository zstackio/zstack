package org.zstack.test.storage.primary.local;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.configuration.DiskOffering;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.host.HostInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.vm.VmInstanceConstant.Capability;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeConstant;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.storage.primary.local.*;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig.Capacity;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.data.SizeUnit;

import java.util.Map;

/*
 * local storage
 * two host A and B
 * host A has enough capacity while B doesn't
 * the only vm running on host B
 * create a big volume(larger than capacity of B) without instantiating it
 * attach the volume to the vm
 * make sure failure occurs
 */
public class TestLocalStorageAttachUnInstantiateDataVolumeToVM {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    LocalStorageSimulatorConfig config;
    long totalSize = SizeUnit.GIGABYTE.toByte(100);


    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/localStorage/TestLocalStorageAttachUnInstantiateDataVolumeToVM.xml", con);
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
        HostInventory host1 = deployer.hosts.get("host1");
        HostInventory host2 = deployer.hosts.get("host2");
        PrimaryStorageInventory local = deployer.primaryStorages.get("local");
        //
        LocalStorageHostRefVO href1 = new LocalStorageHostRefVOFinder().findByPrimaryKey(host1.getUuid(), local.getUuid());
        Assert.assertEquals(href1.getTotalCapacity(), totalSize);
        Assert.assertEquals(href1.getTotalPhysicalCapacity(), totalSize);
        //
        LocalStorageHostRefVO href2 = new LocalStorageHostRefVOFinder().findByPrimaryKey(host2.getUuid(), local.getUuid());
        Assert.assertEquals(href2.getTotalCapacity(), totalSize);
        Assert.assertEquals(href2.getTotalPhysicalCapacity(), totalSize);

        VmInstanceInventory vm = deployer.vms.get("TestVm");
        DiskOfferingInventory diskOfferingInventory1 = deployer.diskOfferings.get("TestDiskOffering1");
        DiskOfferingInventory diskOfferingInventory2 = deployer.diskOfferings.get("TestDiskOffering2");

        // create volume without instantiate it, should be successful
        VolumeInventory volumeInventory2 = api.createDataVolume("2", diskOfferingInventory2.getUuid());
        api.attachVolumeToVm(vm.getUuid(), volumeInventory2.getUuid());

        // create volume and instantiate it
//        thrown.expect(ApiSenderException.class);
//        thrown.expectMessage("required local primary storage");
//        VolumeInventory volumeInventory2 = api.createDataVolumeOnLocalStorage(
//                "2",
//                diskOfferingInventory.getUuid(),
//                local.getUuid(),
//                host1.getUuid(),
//                session);

        // create volume without instantiate it, expect an exception
        thrown.expect(ApiSenderException.class);
        thrown.expectMessage("Unable to attach data volume to a vm");
        VolumeInventory volumeInventory = api.createDataVolume("1", diskOfferingInventory1.getUuid());
        api.attachVolumeToVm(vm.getUuid(), volumeInventory.getUuid());

    }
}
