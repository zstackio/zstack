package org.zstack.test.compute.vm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeStatus;
import org.zstack.header.volume.VolumeType;
import org.zstack.header.volume.VolumeVO;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;

public class TestAttachedVolumeToStoppedVm {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/vm/TestCreateVm.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException {
        DiskOfferingInventory dinv = deployer.diskOfferings.get("TestDataDiskOffering");
        VmInstanceInventory inv = api.listVmInstances(null).get(0);
        VolumeInventory dvol = api.createDataVolume("datavolume", dinv.getUuid());
        api.stopVmInstance(inv.getUuid());
        dvol = api.attachVolumeToVm(inv.getUuid(), dvol.getUuid());
        inv = api.startVmInstance(inv.getUuid());
        Assert.assertEquals(VmInstanceState.Running.toString(), inv.getState());
        VmInstanceVO vm = dbf.findByUuid(inv.getUuid(), VmInstanceVO.class);
        Assert.assertNotNull(vm);
        Assert.assertEquals(VmInstanceState.Running, vm.getState());
        Assert.assertNotNull(vm.getHostUuid());
        Assert.assertEquals(3, vm.getAllVolumes().size());
        for (VolumeVO vovo : vm.getAllVolumes()) {
            if (vovo.getType() == VolumeType.Data) {
                Assert.assertEquals(VolumeStatus.Ready, vovo.getStatus());
            }
        }
    }
}
