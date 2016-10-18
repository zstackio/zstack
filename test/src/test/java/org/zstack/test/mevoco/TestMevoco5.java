package org.zstack.test.mevoco;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.compute.vm.VmGlobalConfig;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.allocator.HostCapacityOverProvisioningManager;
import org.zstack.header.allocator.HostCapacityVO;
import org.zstack.header.host.HostInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.storage.primary.ImageCacheVO;
import org.zstack.header.storage.primary.PrimaryStorageCapacityVO;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.storage.primary.PrimaryStorageOverProvisioningManager;
import org.zstack.header.vm.VmInstanceDeletionPolicyManager.VmInstanceDeletionPolicy;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.mevoco.MevocoGlobalConfig;
import org.zstack.network.service.flat.FlatNetworkServiceSimulatorConfig;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.storage.primary.local.APIGetLocalStorageHostDiskCapacityReply;
import org.zstack.storage.primary.local.APIGetLocalStorageHostDiskCapacityReply.HostDiskCapacity;
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

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 1. create 2 vms
 * 2. delete a vm
 * <p>
 * confirm the memory capacity and disk capacity returned correctly
 */
public class TestMevoco5 {
    CLogger logger = Utils.getLogger(TestMevoco5.class);
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
    long totalSize = SizeUnit.GIGABYTE.toByte(100);

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/mevoco/TestMevoco5.xml", con);
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
        c.avail = totalSize - SizeUnit.GIGABYTE.toByte(11);

        config.capacityMap.put("host1", c);

        MevocoGlobalConfig.DISTRIBUTE_IMAGE.updateValue(false);

        deployer.build();
        api = deployer.getApi();
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        VmGlobalConfig.VM_DELETION_POLICY.updateValue(VmInstanceDeletionPolicy.Direct.toString());
        long totalMemorySize = SizeUnit.GIGABYTE.toByte(32);
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        VmInstanceInventory vm1 = deployer.vms.get("TestVm1");
        HostInventory host = deployer.hosts.get("host1");
        PrimaryStorageInventory local = deployer.primaryStorages.get("local");

        api.stopVmInstance(vm.getUuid());
        HostCapacityVO hcap = dbf.findByUuid(host.getUuid(), HostCapacityVO.class);

        long availMem = totalMemorySize - hostRatioMgr.calculateMemoryByRatio(vm1.getHostUuid(), vm1.getMemorySize());
        Assert.assertEquals(availMem, hcap.getAvailableMemory());

        long isize = 0;
        List<ImageCacheVO> is = dbf.listAll(ImageCacheVO.class);
        for (ImageCacheVO i : is) {
            isize += i.getSize();
        }

        api.destroyVmInstance(vm.getUuid());
        PrimaryStorageCapacityVO pscap = dbf.findByUuid(local.getUuid(), PrimaryStorageCapacityVO.class);
        long availSize = totalSize - isize - pscap.getSystemUsedCapacity() - psRatioMgr.calculateByRatio(vm.getRootVolume().getPrimaryStorageUuid(), vm.getRootVolume().getSize());
        Assert.assertEquals(availSize, pscap.getAvailableCapacity());

        MevocoGlobalConfig.MEMORY_OVER_PROVISIONING_RATIO.updateValue(1);
        TimeUnit.SECONDS.sleep(2);
        hcap = dbf.findByUuid(host.getUuid(), HostCapacityVO.class);
        availMem = totalMemorySize - hostRatioMgr.calculateMemoryByRatio(vm1.getHostUuid(), vm1.getMemorySize());
        Assert.assertEquals(availMem, hcap.getAvailableMemory());

        MevocoGlobalConfig.PRIMARY_STORAGE_OVER_PROVISIONING_RATIO.updateValue(1);
        TimeUnit.SECONDS.sleep(2);
        pscap = dbf.findByUuid(local.getUuid(), PrimaryStorageCapacityVO.class);
        availSize = totalSize - pscap.getSystemUsedCapacity() - isize - psRatioMgr.calculateByRatio(vm1.getRootVolume().getPrimaryStorageUuid(), vm1.getRootVolume().getSize());
        Assert.assertEquals(availSize, pscap.getAvailableCapacity());

        LocalStorageHostRefVO lref = new LocalStorageHostRefVOFinder().findByPrimaryKey(host.getUuid(), local.getUuid());
        availSize = totalSize - lref.getSystemUsedCapacity() - isize - psRatioMgr.calculateByRatio(vm1.getRootVolume().getPrimaryStorageUuid(), vm1.getRootVolume().getSize());
        Assert.assertEquals(availSize, lref.getAvailableCapacity());

        MevocoGlobalConfig.MEMORY_OVER_PROVISIONING_RATIO.updateValue(2);
        TimeUnit.SECONDS.sleep(2);
        hcap = dbf.findByUuid(host.getUuid(), HostCapacityVO.class);
        availMem = totalMemorySize - hostRatioMgr.calculateMemoryByRatio(vm1.getHostUuid(), vm1.getMemorySize());
        Assert.assertEquals(availMem, hcap.getAvailableMemory());

        api.destroyVmInstance(vm1.getUuid());
        pscap = dbf.findByUuid(local.getUuid(), PrimaryStorageCapacityVO.class);
        Assert.assertEquals(pscap.getTotalCapacity() - pscap.getSystemUsedCapacity() - isize, pscap.getAvailableCapacity());

        lref = new LocalStorageHostRefVOFinder().findByPrimaryKey(host.getUuid(), local.getUuid());
        Assert.assertEquals(lref.getTotalCapacity() - lref.getSystemUsedCapacity() - isize, lref.getAvailableCapacity());

        hcap = dbf.findByUuid(host.getUuid(), HostCapacityVO.class);
        Assert.assertEquals(hcap.getTotalMemory(), hcap.getAvailableMemory());

        APIGetLocalStorageHostDiskCapacityReply reply = api.getLocalStorageHostCapacity(local.getUuid(), host.getUuid());
        HostDiskCapacity c = reply.getInventories().get(0);

        Assert.assertEquals(lref.getTotalCapacity(), c.getTotalCapacity());
        Assert.assertEquals(lref.getAvailableCapacity(), c.getAvailableCapacity());
        Assert.assertEquals(lref.getTotalPhysicalCapacity(), c.getTotalPhysicalCapacity());
        Assert.assertEquals(lref.getAvailablePhysicalCapacity(), c.getAvailablePhysicalCapacity());

        reply = api.getLocalStorageHostCapacity(local.getUuid(), null);
        c = reply.getInventories().get(0);

        Assert.assertEquals(lref.getTotalCapacity(), c.getTotalCapacity());
        Assert.assertEquals(lref.getAvailableCapacity(), c.getAvailableCapacity());
        Assert.assertEquals(lref.getTotalPhysicalCapacity(), c.getTotalPhysicalCapacity());
        Assert.assertEquals(lref.getAvailablePhysicalCapacity(), c.getAvailablePhysicalCapacity());
    }
}
