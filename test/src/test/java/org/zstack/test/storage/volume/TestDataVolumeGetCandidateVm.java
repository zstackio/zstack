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

public class TestDataVolumeGetCandidateVm {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/volume/TestCreateDataVolume.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        DiskOfferingInventory dinv = deployer.diskOfferings.get("TestDataDiskOffering");

        VolumeInventory vol = api.createDataVolume("data", dinv.getUuid());
        List<VmInstanceInventory> vms = api.getDataVolumeCandidateVmForAttaching(vol.getUuid());
        Assert.assertEquals(1, vms.size());
        VmInstanceInventory vm = vms.get(0);

        api.attachVolumeToVm(vm.getUuid(), vol.getUuid());
        api.detachVolumeFromVm(vol.getUuid());

        vms = api.getDataVolumeCandidateVmForAttaching(vol.getUuid());
        Assert.assertEquals(1, vms.size());

        api.stopVmInstance(vm.getUuid());
        vms = api.getDataVolumeCandidateVmForAttaching(vol.getUuid());
        Assert.assertEquals(1, vms.size());

        api.destroyVmInstance(vm.getUuid());
        vms = api.getDataVolumeCandidateVmForAttaching(vol.getUuid());
        Assert.assertEquals(0, vms.size());
    }
}
