package org.zstack.test.compute.vm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.configuration.DiskOfferingVO;
import org.zstack.header.configuration.DiskOfferingVO_;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;

public class TestAttachVolumeToVmDeviceId {
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
        SimpleQuery<DiskOfferingVO> dq = dbf.createQuery(DiskOfferingVO.class);
        dq.add(DiskOfferingVO_.name, Op.EQ, "DataOffering1");
        DiskOfferingVO dvo1 = dq.find();

        dq = dbf.createQuery(DiskOfferingVO.class);
        dq.add(DiskOfferingVO_.name, Op.EQ, "DataOffering2");
        DiskOfferingVO dvo2 = dq.find();

        VolumeInventory dinv1 = api.createDataVolume("d1", dvo1.getUuid());
        VolumeInventory dinv2 = api.createDataVolume("d2", dvo2.getUuid());
        Assert.assertNull(dinv1.getDeviceId());
        Assert.assertNull(dinv2.getDeviceId());

        SimpleQuery<VmInstanceVO> vq = dbf.createQuery(VmInstanceVO.class);
        vq.add(VmInstanceVO_.name, Op.EQ, "vm1");
        VmInstanceVO vm1 = vq.find();

        vq = dbf.createQuery(VmInstanceVO.class);
        vq.add(VmInstanceVO_.name, Op.EQ, "vm2");
        VmInstanceVO vm2 = vq.find();

        dinv1 = api.attachVolumeToVm(vm1.getUuid(), dinv1.getUuid());
        Assert.assertEquals(Integer.valueOf(2), dinv1.getDeviceId());

        dinv2 = api.attachVolumeToVm(vm1.getUuid(), dinv2.getUuid());
        Assert.assertEquals(Integer.valueOf(3), dinv2.getDeviceId());

        dinv1 = api.detachVolumeFromVm(dinv1.getUuid());
        dinv1 = api.attachVolumeToVm(vm2.getUuid(), dinv1.getUuid());
        Assert.assertEquals(Integer.valueOf(2), dinv1.getDeviceId());

        VolumeInventory dinv3 = api.createDataVolume("d1", dvo1.getUuid());
        dinv3 = api.attachVolumeToVm(vm1.getUuid(), dinv3.getUuid());
        Assert.assertEquals(Integer.valueOf(2), dinv3.getDeviceId());
    }
}
