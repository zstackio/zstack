package org.zstack.test.mevoco.qos;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.APIDeleteVolumeQosEvent;
import org.zstack.header.volume.APIGetVolumeQosReply;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.APISetVolumeQosEvent;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/**
 * Created by mingjian.deng on 16/12/20.
 */
/**
 * mixed with instance_offering and volume_qos
 * linked with: https://github.com/zxwing/functional-spec/issues/7
 * 1. create a vm with qos
 * 2. assert the qos as instance_offering
 * 3. attatch new disk
 * 4. assert the qos as instance_offering
 * 5. set qos and assert it
 * 6. delete qos and assert it, it's important
 * 7. delete qos and assert it, it's important
 */
public class TestVolumeQosMixed {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    protected static final CLogger logger = Utils.getLogger(TestVolumeQosMixed.class);


    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/kvm/TestCreateVmOnKvmForQos.xml");
        deployer.addSpringConfig("mevocoRelated.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException {
        //1.
        String rootVolumeUuid = deployer.vms.get("Vm_1").getRootVolumeUuid();
        VmInstanceInventory vm = deployer.vms.get("Vm_1");
        //2.
        APIGetVolumeQosReply reply = api.getVmDiskQos(rootVolumeUuid);
        Assert.assertTrue(reply.isSuccess());
        Assert.assertEquals(3000l, reply.getVolumeBandwidth());
        //3.
        DiskOfferingInventory dinv = deployer.diskOfferings.get("TestDataDiskOffering");
        VolumeInventory vol = api.createDataVolume("d1", dinv.getUuid());
        String dataVolume1Uuid = vol.getUuid();
        vol = api.attachVolumeToVm(vm.getUuid(), vol.getUuid());
        Assert.assertEquals(true, vol.isAttached());
        //4.
        reply = api.getVmDiskQos(dataVolume1Uuid);
        Assert.assertTrue(reply.isSuccess());
        Assert.assertEquals(3000l, reply.getVolumeBandwidth());
        //5.
        APISetVolumeQosEvent event = api.setDiskQos(dataVolume1Uuid, 10000l);
        Assert.assertTrue(event.isSuccess());
        reply = api.getVmDiskQos(dataVolume1Uuid);
        Assert.assertTrue(reply.isSuccess());
        Assert.assertEquals(10000l, reply.getVolumeBandwidth());
        //6.
        APIDeleteVolumeQosEvent event1 = api.deleteDiskQos(dataVolume1Uuid);
        Assert.assertTrue(event1.isSuccess());
        reply = api.getVmDiskQos(dataVolume1Uuid);
        Assert.assertTrue(reply.isSuccess());
        // here should be equal 0 and equal 3000 after restart vm, but we could't simulate it...
        Assert.assertEquals(3000l, reply.getVolumeBandwidth());
        //7.
        event1 = api.deleteDiskQos(dataVolume1Uuid);
        Assert.assertTrue(event1.isSuccess());
        reply = api.getVmDiskQos(dataVolume1Uuid);
        Assert.assertTrue(reply.isSuccess());
        Assert.assertEquals(3000l, reply.getVolumeBandwidth());

    }
}
