package org.zstack.test.compute.vm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.*;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeType;
import org.zstack.header.volume.VolumeVO;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;

public class TestCreateVm2 {
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
        /*
        InstanceOfferingInventory ioinv = api.listInstanceOffering(null).get(0);
        ImageInventory iminv = api.listImage(null).get(0);
        List<DiskOfferingInventory> dinvs = api.listDiskOffering(null);
        List<L3NetworkInventory> nwinvs = api.listL3Network(null);
        List<String> nws = new ArrayList<String>(nwinvs.size());
        for (L3NetworkInventory nwinv : nwinvs) {
            nws.add(nwinv.getUuid());
        }
        */
        InstanceOfferingInventory ioinv = api.listInstanceOffering(null).get(0);
        ImageInventory iminv = api.listImage(null).get(0);
        //
        L3NetworkInventory l3v = api.listL3Network(null).get(0);
        VmInstanceInventory inv = api.listVmInstances(null).get(0);
        Assert.assertEquals(inv.getInstanceOfferingUuid(), ioinv.getUuid());
        Assert.assertEquals(inv.getImageUuid(), iminv.getUuid());
        //
        Assert.assertEquals(inv.getDefaultL3NetworkUuid(), l3v.getUuid());
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
