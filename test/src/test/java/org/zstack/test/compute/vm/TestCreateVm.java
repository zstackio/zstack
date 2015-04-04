package org.zstack.test.compute.vm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.vm.*;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeType;
import org.zstack.header.volume.VolumeVO;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;

public class TestCreateVm {
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
    public void test() throws ApiSenderException, InterruptedException {
        InstanceOfferingInventory ioinv = api.listInstanceOffering(null).get(0);
        ImageInventory iminv = api.listImage(null).get(0);
        VmInstanceInventory inv = api.listVmInstances(null).get(0);
        Assert.assertEquals(inv.getInstanceOfferingUuid(), ioinv.getUuid());
        Assert.assertEquals(inv.getImageUuid(), iminv.getUuid());
        Assert.assertEquals(VmInstanceState.Running.toString(), inv.getState());
        Assert.assertEquals(3, inv.getVmNics().size());
        VmInstanceVO vm = dbf.findByUuid(inv.getUuid(), VmInstanceVO.class);
        Assert.assertNotNull(vm);
        Assert.assertEquals(VmInstanceState.Running, vm.getState());
        for (VmNicInventory nic : inv.getVmNics()) {
            VmNicVO nvo = dbf.findByUuid(nic.getUuid(), VmNicVO.class);
            Assert.assertNotNull(nvo);
        }
        VolumeVO root = dbf.findByUuid(inv.getRootVolumeUuid(), VolumeVO.class);
        Assert.assertNotNull(root);
        for (VolumeInventory v : inv.getAllVolumes()) {
            if (v.getType().equals(VolumeType.Data.toString())) {
                VolumeVO data = dbf.findByUuid(v.getUuid(), VolumeVO.class);
                Assert.assertNotNull(data);
            }
        }
    }

}
