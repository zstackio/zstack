package org.zstack.test.compute.vm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;

import java.util.Arrays;
import java.util.List;

/* use the same data volume offering to create multiple data volume for vm */
public class TestCreateVmWithMultipleSameDataVolume {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/vm/TestCreateVmWithMultipleSameDataVolume.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        ImageInventory img = deployer.images.get("TestImage");
        InstanceOfferingInventory inso = deployer.instanceOfferings.get("TestInstanceOffering");
        L3NetworkInventory l3 = deployer.l3Networks.get("TestL3Network1");
        DiskOfferingInventory disk = deployer.diskOfferings.get("TestDataDiskOffering");
        VmInstanceInventory vm = new VmInstanceInventory();
        vm.setDescription("TestVm");
        vm.setName("TestVm");
        vm.setType(VmInstanceConstant.USER_VM_TYPE);
        vm.setInstanceOfferingUuid(inso.getUuid());
        vm.setImageUuid(img.getUuid());

        List<String> diskOfferings = Arrays.asList(disk.getUuid(), disk.getUuid(), disk.getUuid());
        List<String> l3uuids = Arrays.asList(l3.getUuid());
        vm = api.createVmByFullConfig(vm, null, l3uuids, diskOfferings);
        Assert.assertEquals(4, vm.getAllVolumes().size());
    }

}
