package org.zstack.test.mevoco;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.compute.vm.VmGlobalConfig;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.allocator.HostCapacityVO;
import org.zstack.header.host.HostInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.storage.primary.ImageCacheVO;
import org.zstack.header.storage.primary.PrimaryStorageCapacityVO;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.vm.VmInstanceDeletionPolicyManager.VmInstanceDeletionPolicy;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.mevoco.MevocoGlobalConfig;
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

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 1. destroy all vms
 * 2. change PRIMARY_STORAGE_OVER_PROVISIONING_RATIO
 * <p>
 * confirm the primary storage capacity is correct
 * <p>
 * confirm the memory capacity and disk capacity returned correctly
 */
public class TestMevoco6 {
    CLogger logger = Utils.getLogger(TestMevoco6.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    LocalStorageSimulatorConfig config;
    FlatNetworkServiceSimulatorConfig fconfig;
    KVMSimulatorConfig kconfig;
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

        Capacity c = new Capacity();
        c.total = totalSize;
        c.avail = totalSize;

        config.capacityMap.put("host1", c);

        MevocoGlobalConfig.DISTRIBUTE_IMAGE.updateValue(false);

        deployer.build();
        api = deployer.getApi();
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        VmGlobalConfig.VM_DELETION_POLICY.updateValue(VmInstanceDeletionPolicy.Direct.toString());
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        VmInstanceInventory vm1 = deployer.vms.get("TestVm1");
        HostInventory host = deployer.hosts.get("host1");
        PrimaryStorageInventory local = deployer.primaryStorages.get("local");

        long iused = 0;
        List<ImageCacheVO> ics = dbf.listAll(ImageCacheVO.class);
        for (ImageCacheVO ic : ics) {
            iused += ic.getSize();
        }

        api.destroyVmInstance(vm.getUuid());
        api.destroyVmInstance(vm1.getUuid());

        PrimaryStorageCapacityVO pscap = dbf.findByUuid(local.getUuid(), PrimaryStorageCapacityVO.class);
        Assert.assertEquals(pscap.getAvailableCapacity(), pscap.getTotalCapacity() - iused);
        LocalStorageHostRefVO ref = new LocalStorageHostRefVOFinder().findByPrimaryKey(host.getUuid(), local.getUuid());
        Assert.assertEquals(ref.getTotalCapacity() - iused, ref.getAvailableCapacity());

        MevocoGlobalConfig.PRIMARY_STORAGE_OVER_PROVISIONING_RATIO.updateValue(1);
        TimeUnit.SECONDS.sleep(2);
        pscap = dbf.findByUuid(local.getUuid(), PrimaryStorageCapacityVO.class);
        Assert.assertEquals(pscap.getAvailableCapacity(), pscap.getTotalCapacity() - iused);
        ref = new LocalStorageHostRefVOFinder().findByPrimaryKey(host.getUuid(), local.getUuid());
        Assert.assertEquals(ref.getTotalCapacity() - iused, ref.getAvailableCapacity());

        MevocoGlobalConfig.PRIMARY_STORAGE_OVER_PROVISIONING_RATIO.updateValue(2);
        TimeUnit.SECONDS.sleep(2);
        pscap = dbf.findByUuid(local.getUuid(), PrimaryStorageCapacityVO.class);
        Assert.assertEquals(pscap.getAvailableCapacity(), pscap.getTotalCapacity() - iused);
        ref = new LocalStorageHostRefVOFinder().findByPrimaryKey(host.getUuid(), local.getUuid());
        Assert.assertEquals(ref.getTotalCapacity() - iused, ref.getAvailableCapacity());

        MevocoGlobalConfig.MEMORY_OVER_PROVISIONING_RATIO.updateValue(1);
        TimeUnit.SECONDS.sleep(2);
        HostCapacityVO hcap = dbf.findByUuid(host.getUuid(), HostCapacityVO.class);
        Assert.assertEquals(hcap.getTotalMemory(), hcap.getAvailableMemory());

        MevocoGlobalConfig.MEMORY_OVER_PROVISIONING_RATIO.updateValue(2);
        TimeUnit.SECONDS.sleep(2);
        hcap = dbf.findByUuid(host.getUuid(), HostCapacityVO.class);
        Assert.assertEquals(hcap.getTotalMemory(), hcap.getAvailableMemory());
    }
}
