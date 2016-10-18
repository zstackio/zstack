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
import org.zstack.header.volume.VolumeDeletionPolicyManager.VolumeDeletionPolicy;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.network.service.flat.FlatNetworkServiceSimulatorConfig;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.storage.primary.local.LocalStorageHostRefVO;
import org.zstack.storage.primary.local.LocalStorageHostRefVOFinder;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig.Capacity;
import org.zstack.storage.volume.VolumeGlobalConfig;
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
 * 1. create a vm with mevoco setting
 * 2. create and attach two data volumes
 * 3. delete each data volume and finally destroy the vm
 * <p>
 * confirm each time the primary storage capacity is correct
 */
public class TestMevoco18 {
    CLogger logger = Utils.getLogger(TestMevoco18.class);
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
        deployer = new Deployer("deployerXml/mevoco/TestMevoco18.xml", con);
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

        deployer.build();
        api = deployer.getApi();
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        PrimaryStorageInventory local = deployer.primaryStorages.get("local");
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        VolumeInventory root = vm.getRootVolume();
        DiskOfferingInventory doffering = deployer.diskOfferings.get("TestRootDiskOffering");
        VolumeInventory data1 = api.createDataVolume("data1", doffering.getUuid());
        VolumeInventory data2 = api.createDataVolume("data2", doffering.getUuid());
        data1 = api.attachVolumeToVm(vm.getUuid(), data1.getUuid());
        data2 = api.attachVolumeToVm(vm.getUuid(), data2.getUuid());

        PrimaryStorageInventory ps = deployer.primaryStorages.get("local");
        PrimaryStorageCapacityVO cap1 = dbf.findByUuid(ps.getUuid(), PrimaryStorageCapacityVO.class);
        HostInventory host = deployer.hosts.get("host1");
        LocalStorageHostRefVO ref1 = new LocalStorageHostRefVOFinder().findByPrimaryKey(host.getUuid(), local.getUuid());

        long size = psRatioMgr.calculateByRatio(ps.getUuid(), data1.getSize());
        VolumeGlobalConfig.VOLUME_DELETION_POLICY.updateValue(VolumeDeletionPolicy.Direct.toString());
        api.deleteDataVolume(data1.getUuid());
        TimeUnit.SECONDS.sleep(3);
        PrimaryStorageCapacityVO cap2 = dbf.findByUuid(ps.getUuid(), PrimaryStorageCapacityVO.class);
        LocalStorageHostRefVO ref2 = new LocalStorageHostRefVOFinder().findByPrimaryKey(host.getUuid(), local.getUuid());
        Assert.assertEquals(cap2.getAvailableCapacity(), cap1.getAvailableCapacity() + size);
        Assert.assertEquals(ref2.getAvailableCapacity(), ref1.getAvailableCapacity() + size);

        size = psRatioMgr.calculateByRatio(ps.getUuid(), data2.getSize());
        VolumeGlobalConfig.VOLUME_DELETION_POLICY.updateValue(VolumeDeletionPolicy.Delay.toString());
        VolumeGlobalConfig.VOLUME_EXPUNGE_INTERVAL.updateValue(1);
        VolumeGlobalConfig.VOLUME_EXPUNGE_PERIOD.updateValue(1);
        api.deleteDataVolume(data2.getUuid());
        TimeUnit.SECONDS.sleep(3);
        PrimaryStorageCapacityVO cap3 = dbf.findByUuid(ps.getUuid(), PrimaryStorageCapacityVO.class);
        LocalStorageHostRefVO ref3 = new LocalStorageHostRefVOFinder().findByPrimaryKey(host.getUuid(), local.getUuid());
        Assert.assertEquals(cap3.getAvailableCapacity(), cap2.getAvailableCapacity() + size);
        Assert.assertEquals(ref3.getAvailableCapacity(), ref2.getAvailableCapacity() + size);

        size = psRatioMgr.calculateByRatio(ps.getUuid(), root.getSize());
        VmGlobalConfig.VM_EXPUNGE_INTERVAL.updateValue(1);
        VmGlobalConfig.VM_EXPUNGE_PERIOD.updateValue(1);
        api.destroyVmInstance(vm.getUuid());
        TimeUnit.SECONDS.sleep(3);
        PrimaryStorageCapacityVO cap4 = dbf.findByUuid(ps.getUuid(), PrimaryStorageCapacityVO.class);
        LocalStorageHostRefVO ref4 = new LocalStorageHostRefVOFinder().findByPrimaryKey(host.getUuid(), local.getUuid());
        Assert.assertEquals(cap4.getAvailableCapacity(), cap3.getAvailableCapacity() + size);
        Assert.assertEquals(ref4.getAvailableCapacity(), ref3.getAvailableCapacity() + size);

        VmInstanceInventory vm1 = deployer.vms.get("TestVm1");
        size = psRatioMgr.calculateByRatio(ps.getUuid(), vm1.getRootVolume().getSize());
        VmGlobalConfig.VM_DELETION_POLICY.updateValue(VmInstanceDeletionPolicy.Direct.toString());
        api.destroyVmInstance(vm1.getUuid());
        PrimaryStorageCapacityVO cap5 = dbf.findByUuid(ps.getUuid(), PrimaryStorageCapacityVO.class);
        LocalStorageHostRefVO ref5 = new LocalStorageHostRefVOFinder().findByPrimaryKey(host.getUuid(), local.getUuid());
        Assert.assertEquals(cap5.getAvailableCapacity(), cap4.getAvailableCapacity() + size);
        Assert.assertEquals(ref5.getAvailableCapacity(), ref4.getAvailableCapacity() + size);

        VmInstanceInventory vm2 = deployer.vms.get("TestVm2");
        size = psRatioMgr.calculateByRatio(ps.getUuid(), vm1.getRootVolume().getSize());
        VolumeGlobalConfig.VOLUME_DELETION_POLICY.updateValue(VolumeDeletionPolicy.Direct.toString());
        api.destroyVmInstance(vm2.getUuid());
        PrimaryStorageCapacityVO cap6 = dbf.findByUuid(ps.getUuid(), PrimaryStorageCapacityVO.class);
        LocalStorageHostRefVO ref6 = new LocalStorageHostRefVOFinder().findByPrimaryKey(host.getUuid(), local.getUuid());
        Assert.assertEquals(cap6.getAvailableCapacity(), cap5.getAvailableCapacity() + size);
        Assert.assertEquals(ref6.getAvailableCapacity(), ref5.getAvailableCapacity() + size);

        api.deleteHost(host.getUuid());
        PrimaryStorageCapacityVO cap7 = dbf.findByUuid(ps.getUuid(), PrimaryStorageCapacityVO.class);
        Assert.assertEquals(0, cap7.getAvailableCapacity());
        Assert.assertEquals(0, cap7.getTotalCapacity());
    }
}
