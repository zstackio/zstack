package org.zstack.test.storage.volume;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;

import java.util.List;

public class TestVmGetAttachableVolume {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/volume/TestVmGetAttachableVolume.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        DiskOfferingInventory dinv = deployer.diskOfferings.get("TestDataDiskOffering");
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        VolumeInventory vol = api.createDataVolume("data", dinv.getUuid());

        List<VolumeInventory> vols = api.getVmAttachableVolume(vm.getUuid());
        Assert.assertEquals(1, vols.size());

        api.attachVolumeToVm(vm.getUuid(), vol.getUuid());
        vols = api.getVmAttachableVolume(vm.getUuid());
        Assert.assertEquals(0, vols.size());

        api.detachVolumeFromVm(vol.getUuid());
        vols = api.getVmAttachableVolume(vm.getUuid());
        Assert.assertEquals(1, vols.size());

        api.createDataVolume("data", dinv.getUuid());
        vols = api.getVmAttachableVolume(vm.getUuid());
        Assert.assertEquals(2, vols.size());

        api.attachVolumeToVm(vm.getUuid(), vol.getUuid());
        vols = api.getVmAttachableVolume(vm.getUuid());
        Assert.assertEquals(1, vols.size());
    }
}
