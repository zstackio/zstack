package org.zstack.test.mevoco;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.compute.vm.VmGlobalConfig;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.allocator.HostCapacityOverProvisioningManager;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.host.HostInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.storage.primary.PrimaryStorageCapacityVO;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.storage.primary.PrimaryStorageOverProvisioningManager;
import org.zstack.header.vm.VmInstanceDeletionPolicyManager.VmInstanceDeletionPolicy;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeStatus;
import org.zstack.header.volume.VolumeVO;
import org.zstack.network.service.flat.FlatNetworkServiceSimulatorConfig;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.storage.primary.local.LocalStorageHostRefVO;
import org.zstack.storage.primary.local.LocalStorageHostRefVOFinder;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig.Capacity;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.logging.CLogger;

import java.util.concurrent.TimeUnit;

/**
 * 1. create a 3 vm on the host1
 * 2. attach 2 data volumes to vm1 which is on the host1
 * 3. create 1 vm(vm3) on the host2 and attach 2 data volumes to the vm3
 * 4. delete the host1
 * <p>
 * confirm vms and data volumes are deleted
 * confirm the total capacity of the primary storage is correct
 * confirm the vm3 and its data volumes are not effected by the host1 deletion
 * <p>
 * 5. delete the host2
 * <p>
 * confirm the capacity of the primary storage becomes zero
 */
public class TestMevoco19 {
    CLogger logger = Utils.getLogger(TestMevoco19.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    LocalStorageSimulatorConfig config;
    FlatNetworkServiceSimulatorConfig fconfig;
    KVMSimulatorConfig kconfig;
    PrimaryStorageOverProvisioningManager psRatioMgr;
    HostCapacityOverProvisioningManager hostRatioMgr;
    long totalSize = SizeUnit.GIGABYTE.toByte(1000);

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/mevoco/TestMevoco19.xml", con);
        deployer.addSpringConfig("mevocoRelated.xml");
        deployer.load();

        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(LocalStorageSimulatorConfig.class);
        fconfig = loader.getComponent(FlatNetworkServiceSimulatorConfig.class);
        kconfig = loader.getComponent(KVMSimulatorConfig.class);
        psRatioMgr = loader.getComponent(PrimaryStorageOverProvisioningManager.class);
        hostRatioMgr = loader.getComponent(HostCapacityOverProvisioningManager.class);

        Capacity c = new Capacity();
        c.total = totalSize;
        c.avail = totalSize;

        config.capacityMap.put("host1", c);
        config.capacityMap.put("host2", c);

        deployer.build();
        api = deployer.getApi();
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        VmGlobalConfig.VM_DELETION_POLICY.updateValue(VmInstanceDeletionPolicy.Direct.toString());
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        VmInstanceInventory vm1 = deployer.vms.get("TestVm1");
        VmInstanceInventory vm2 = deployer.vms.get("TestVm2");
        VmInstanceInventory vm3 = deployer.vms.get("TestVm3");

        DiskOfferingInventory doffering = deployer.diskOfferings.get("DiskOffering");
        VolumeInventory data1 = api.createDataVolume("data1", doffering.getUuid());
        VolumeInventory data11 = api.createDataVolume("data2", doffering.getUuid());
        api.attachVolumeToVm(vm.getUuid(), data1.getUuid());
        api.attachVolumeToVm(vm.getUuid(), data11.getUuid());

        VolumeInventory data31 = api.createDataVolume("data3", doffering.getUuid());
        VolumeInventory data32 = api.createDataVolume("data4", doffering.getUuid());
        api.attachVolumeToVm(vm3.getUuid(), data31.getUuid());
        api.attachVolumeToVm(vm3.getUuid(), data32.getUuid());


        HostInventory host1 = deployer.hosts.get("host1");
        HostInventory host2 = deployer.hosts.get("host2");
        api.deleteHost(host1.getUuid());
        TimeUnit.SECONDS.sleep(2);
        PrimaryStorageInventory local = deployer.primaryStorages.get("local");

        LocalStorageHostRefVO ref2 = new LocalStorageHostRefVOFinder().findByPrimaryKey(host2.getUuid(), local.getUuid());
        PrimaryStorageCapacityVO cap = dbf.findByUuid(local.getUuid(), PrimaryStorageCapacityVO.class);
        Assert.assertEquals(ref2.getAvailableCapacity(), cap.getAvailableCapacity());
        Assert.assertEquals(ref2.getTotalCapacity(), cap.getTotalCapacity());

        VmInstanceVO vmvo = dbf.findByUuid(vm.getUuid(), VmInstanceVO.class);
        Assert.assertNull(vmvo);
        VmInstanceVO vmvo1 = dbf.findByUuid(vm1.getUuid(), VmInstanceVO.class);
        Assert.assertNull(vmvo1);
        VmInstanceVO vmvo2 = dbf.findByUuid(vm2.getUuid(), VmInstanceVO.class);
        Assert.assertNull(vmvo2);

        VolumeVO dvo1 = dbf.findByUuid(data1.getUuid(), VolumeVO.class);
        Assert.assertNull(dvo1);
        VolumeVO dvo11 = dbf.findByUuid(data11.getUuid(), VolumeVO.class);
        Assert.assertNull(dvo11);

        VmInstanceVO vmvo3 = dbf.findByUuid(vm3.getUuid(), VmInstanceVO.class);
        Assert.assertEquals(VmInstanceState.Running, vmvo3.getState());

        VolumeVO dvo31 = dbf.findByUuid(data31.getUuid(), VolumeVO.class);
        Assert.assertEquals(VolumeStatus.Ready, dvo31.getStatus());
        VolumeVO dvo32 = dbf.findByUuid(data32.getUuid(), VolumeVO.class);
        Assert.assertEquals(VolumeStatus.Ready, dvo32.getStatus());

        api.deleteHost(host2.getUuid());
        TimeUnit.SECONDS.sleep(2);
        cap = dbf.findByUuid(local.getUuid(), PrimaryStorageCapacityVO.class);
        Assert.assertEquals(0, cap.getAvailableCapacity());
        Assert.assertEquals(0, cap.getTotalCapacity());

        Assert.assertEquals(0, dbf.count(VmInstanceVO.class));
        Assert.assertEquals(0, dbf.count(VolumeVO.class));
    }
}
