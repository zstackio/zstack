package org.zstack.test.compute.vm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeType;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;

public class TestAttachDataVolumeFromDestoryedVm {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/vm/TestAttachVolumeToVmDeviceId.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        VmInstanceInventory vm1 = deployer.vms.get("vm1");
        VolumeInventory dvol1 = null;
        for (VolumeInventory vol : vm1.getAllVolumes()) {
            if (vol.getType().equals(VolumeType.Data.toString())) {
                dvol1 = vol;
                break;
            }
        }

        VmInstanceInventory vm2 = deployer.vms.get("vm2");
        api.destroyVmInstance(vm1.getUuid());
        dvol1 = api.attachVolumeToVm(vm2.getUuid(), dvol1.getUuid());
        Assert.assertEquals(Integer.valueOf(2), dvol1.getDeviceId());
    }
}
