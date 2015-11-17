package org.zstack.test.compute.vm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.compute.vm.VmGlobalConfig;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.allocator.HostCapacityVO;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.VmInstance;
import org.zstack.header.vm.VmInstanceDeletionPolicyManager.VmInstanceDeletionPolicy;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.volume.VolumeVO;
import org.zstack.storage.volume.VolumeGlobalConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;

import java.util.concurrent.TimeUnit;

/**
 * 1. create 3 vm
 * 2. delete vm1
 *
 * confirm the vm1 changed to Destroyed state
 *
 * 3. recover vm1
 *
 * confirm the vm1 recover to state Stopped
 *
 * 4. attach a l3 to vm1 and start it
 *
 * confirm the vm1 starts successfully
 *
 * 5. destroy vm1
 * 6. update expunge interval and period to 1s
 *
 * confirm the vm1 is deleted
 *
 * 7. update vm deletion policy to Direct
 * 8. delete vm2
 *
 * confirm the vm2 is deleted
 *
 * 9. update vm deletion policy to Never
 * 10. delete vm3
 *
 * confirm vm3 changed to Destroyed state
 *
 * 11. sleep 3s
 *
 * confirm vm3 is still there
 *
 * 12. update volume expunge interval and period to 1s
 *
 * confirm root volume of the vm3 is still there
 *
 * 13. delete and expunge the vm4
 *
 * confirm the vm4 is expunged
 */
public class TestDestroyVm {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/vm/TestDestroyVm.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }
    
    @Test
    public void test() throws ApiSenderException, InterruptedException {
        VmInstanceInventory vm1 = deployer.vms.get("TestVm");
        VmInstanceInventory vm2 = api.createVmFromClone(vm1);
        VmInstanceInventory vm3 = api.createVmFromClone(vm1);
        VmInstanceInventory vm4 = api.createVmFromClone(vm1);

        api.destroyVmInstance(vm1.getUuid());
        VmInstanceVO vmvo1 = dbf.findByUuid(vm1.getUuid(), VmInstanceVO.class);
        Assert.assertNotNull(vmvo1);
        Assert.assertEquals(VmInstanceState.Destroyed, vmvo1.getState());
        vm1 = VmInstanceInventory.valueOf(vmvo1);
        Assert.assertNotNull(vm1.getRootVolume());

        vm1 = api.recoverVm(vm1.getUuid(), null);
        Assert.assertEquals(VmInstanceState.Stopped.toString(), vm1.getState());
        Assert.assertNull(vm1.getHostUuid());
        L3NetworkInventory l3 = deployer.l3Networks.get("TestL3Network1");
        api.attachNic(vm1.getUuid(), l3.getUuid());
        api.startVmInstance(vm1.getUuid());

        api.destroyVmInstance(vm1.getUuid());
        VmGlobalConfig.VM_EXPUNGE_PERIOD.updateValue(1);
        VmGlobalConfig.VM_EXPUNGE_INTERVAL.updateValue(1);

        TimeUnit.SECONDS.sleep(3);
        vmvo1 = dbf.findByUuid(vm1.getUuid(), VmInstanceVO.class);
        Assert.assertNull(vmvo1);

        VmGlobalConfig.VM_DELETION_POLICY.updateValue(VmInstanceDeletionPolicy.Direct.toString());
        api.destroyVmInstance(vm2.getUuid());
        VmInstanceVO vmvo2 = dbf.findByUuid(vm2.getUuid(), VmInstanceVO.class);
        Assert.assertNull(vmvo2);

        VmGlobalConfig.VM_DELETION_POLICY.updateValue(VmInstanceDeletionPolicy.Never.toString());
        api.destroyVmInstance(vm3.getUuid());
        TimeUnit.SECONDS.sleep(3);

        VmInstanceVO vmvo3 = dbf.findByUuid(vm3.getUuid(), VmInstanceVO.class);
        Assert.assertNotNull(vmvo3);
        Assert.assertEquals(VmInstanceState.Destroyed, vmvo3.getState());
        vm3 = VmInstanceInventory.valueOf(vmvo3);
        Assert.assertNotNull(vm3.getRootVolume());

        VolumeGlobalConfig.VOLUME_EXPUNGE_INTERVAL.updateValue(1);
        VolumeGlobalConfig.VOLUME_EXPUNGE_PERIOD.updateValue(1);
        TimeUnit.SECONDS.sleep(3);
        VolumeVO root3 = dbf.findByUuid(vm3.getRootVolumeUuid(), VolumeVO.class);
        Assert.assertNotNull(root3);

        VmGlobalConfig.VM_DELETION_POLICY.updateValue(VmInstanceDeletionPolicy.Delay.toString());
        TimeUnit.SECONDS.sleep(3);
        vmvo3 = dbf.findByUuid(vm3.getUuid(), VmInstanceVO.class);
        Assert.assertNull(vmvo3);

        VmGlobalConfig.VM_EXPUNGE_PERIOD.updateValue(1000);
        VmGlobalConfig.VM_EXPUNGE_INTERVAL.updateValue(100);
        api.destroyVmInstance(vm4.getUuid());
        api.expungeVm(vm4.getUuid(), null);
        VmInstanceVO vmvo4 = dbf.findByUuid(vm4.getUuid(), VmInstanceVO.class);
        Assert.assertNull(vmvo4);
    }
}
