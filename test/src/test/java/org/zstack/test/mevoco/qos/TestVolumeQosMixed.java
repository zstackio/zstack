package org.zstack.test.mevoco.qos;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.vm.CloneVmInstanceResults;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.APIDeleteVolumeQosEvent;
import org.zstack.header.volume.APIGetVolumeQosReply;
import org.zstack.header.volume.APISetVolumeQosEvent;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.storage.backup.imagestore.ImageStoreBackupStorageSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.VmCreator;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;

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
 * 6. delete qos and assert it, it's back to -1
 * 7. set qos and clone it, then check it's qos as the initial vm
 */
public class TestVolumeQosMixed {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    VmCreator creator;
    List<String> names = new ArrayList<>();

    protected static final CLogger logger = Utils.getLogger(TestVolumeQosMixed.class);


    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/kvm/TestCreateVmOnKvmForQos.xml");
        deployer.addSpringConfig("mevocoRelated.xml");
        deployer.addSpringConfig("imagestore.xml");
        deployer.addSpringConfig("ImageStoreBackupStorageSimulator.xml");
        deployer.addSpringConfig("ImageStorePrimaryStorageSimulator.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        loader.getComponent(ImageStoreBackupStorageSimulatorConfig.class);
        creator = new VmCreator(api);
        names.add("test1");
    }

    @Test
    public void test() throws ApiSenderException {
        //1. create a vm with qos
        VmInstanceInventory vm = deployer.vms.get("Vm_1");
        String rootVolumeUuid = vm.getRootVolumeUuid();

        //2. assert the qos as instance_offering
        APIGetVolumeQosReply reply = api.getVmDiskQos(rootVolumeUuid);
        Assert.assertTrue(reply.isSuccess());
        Assert.assertEquals(3000l, reply.getVolumeBandwidth());

        //3. clone it, then check it's qos as the initial vm
        CloneVmInstanceResults res = creator.cloneVm(names, vm.getUuid());
        VmInstanceInventory clonevm = res.getInventories().get(0).getInventory();
        reply = api.getVmDiskQos(clonevm.getRootVolumeUuid());
        Assert.assertTrue(reply.isSuccess());
        Assert.assertEquals(3000l, reply.getVolumeBandwidth());

        //4. attatch new disk
        DiskOfferingInventory dinv = deployer.diskOfferings.get("TestDataDiskOffering");
        VolumeInventory vol = api.createDataVolume("d1", dinv.getUuid());
        String dataVolume1Uuid = vol.getUuid();
        vol = api.attachVolumeToVm(vm.getUuid(), vol.getUuid());
        Assert.assertEquals(true, vol.isAttached());

        //5. assert the qos as instance_offering
        reply = api.getVmDiskQos(dataVolume1Uuid);
        Assert.assertTrue(reply.isSuccess());
        Assert.assertEquals(3000l, reply.getVolumeBandwidth());

        //6. set qos and assert it
        APISetVolumeQosEvent event = api.setDiskQos(dataVolume1Uuid, 10000l);
        Assert.assertTrue(event.isSuccess());
        reply = api.getVmDiskQos(dataVolume1Uuid);
        Assert.assertTrue(reply.isSuccess());
        Assert.assertEquals(10000l, reply.getVolumeBandwidth());

        //7. delete qos and assert it, it's back to -1
        APIDeleteVolumeQosEvent event1 = api.deleteDiskQos(dataVolume1Uuid);
        Assert.assertTrue(event1.isSuccess());
        reply = api.getVmDiskQos(dataVolume1Uuid);
        Assert.assertTrue(reply.isSuccess());
        // here should be equal -1
        Assert.assertEquals(-1l, reply.getVolumeBandwidth());

        //8. set qos and clone it, then check it's qos as the initial vm
        event = api.setDiskQos(rootVolumeUuid, 20000l);
        Assert.assertTrue(event.isSuccess());
        res = creator.cloneVm(names, vm.getUuid());

        reply = api.getVmDiskQos(rootVolumeUuid);
        Assert.assertTrue(reply.isSuccess());
        Assert.assertEquals(20000l, reply.getVolumeBandwidth());

        clonevm = res.getInventories().get(0).getInventory();
        reply = api.getVmDiskQos(clonevm.getRootVolumeUuid());
        Assert.assertTrue(reply.isSuccess());
        Assert.assertEquals(20000l, reply.getVolumeBandwidth());


        //9. delete qos and clone it, then check it's qos as the initial vm
        event1 = api.deleteDiskQos(rootVolumeUuid);
        Assert.assertTrue(event1.isSuccess());

        reply = api.getVmDiskQos(rootVolumeUuid);
        Assert.assertTrue(reply.isSuccess());
        Assert.assertEquals(-1l, reply.getVolumeBandwidth());
        res = creator.cloneVm(names, vm.getUuid());

        clonevm = res.getInventories().get(0).getInventory();
        reply = api.getVmDiskQos(clonevm.getRootVolumeUuid());
        Assert.assertTrue(reply.isSuccess());
        Assert.assertEquals(-1l, reply.getVolumeBandwidth());

    }
}
