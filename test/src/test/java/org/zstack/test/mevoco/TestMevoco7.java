package org.zstack.test.mevoco;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.allocator.HostCapacityVO;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.host.HostInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.storage.primary.PrimaryStorage;
import org.zstack.header.storage.primary.PrimaryStorageCapacityVO;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.volume.*;
import org.zstack.mevoco.MevocoGlobalConfig;
import org.zstack.network.service.flat.FlatNetworkServiceSimulatorConfig;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.storage.primary.local.LocalStorageHostRefVO;
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
 *
 * confirm the primary storage capacity is correct
 *
 * confirm the memory capacity and disk capacity returned correctly
 *
 */
public class TestMevoco7 {
    CLogger logger = Utils.getLogger(TestMevoco7.class);
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
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("mevoco.xml");
        deployer.addSpringConfig("localStorage.xml");
        deployer.addSpringConfig("localStorageSimulator.xml");
        deployer.addSpringConfig("flatNetworkServiceSimulator.xml");
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

        deployer.build();
        api = deployer.getApi();
        session = api.loginAsAdmin();
    }

    private long usedVolumeSize() {
        long used = 0;
        SimpleQuery<VolumeVO> q = dbf.createQuery(VolumeVO.class);
        q.add(VolumeVO_.status, Op.EQ, VolumeStatus.Ready);
        List<VolumeVO> vols = q.list();
        for (VolumeVO v : vols) {
            used += v.getSize();
        }
        return used;
    }

    
	@Test
	public void test() throws ApiSenderException, InterruptedException {
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        VmInstanceInventory vm1 = deployer.vms.get("TestVm1");
        HostInventory host = deployer.hosts.get("host1");
        PrimaryStorageInventory ps = deployer.primaryStorages.get("local");

        MevocoGlobalConfig.PRIMARY_STORAGE_OVER_PROVISIONING_RATIO.updateValue(2.5);
        TimeUnit.SECONDS.sleep(2);

        long used = Math.round(usedVolumeSize() / MevocoGlobalConfig.PRIMARY_STORAGE_OVER_PROVISIONING_RATIO.value(Double.class));
        PrimaryStorageCapacityVO pscap = dbf.findByUuid(ps.getUuid(), PrimaryStorageCapacityVO.class);
        Assert.assertEquals(pscap.getTotalCapacity() - used, pscap.getAvailableCapacity());

        LocalStorageHostRefVO ref = dbf.findByUuid(host.getUuid(), LocalStorageHostRefVO.class);
        Assert.assertEquals(ref.getTotalCapacity() - used, ref.getAvailableCapacity());

        int num = 12;
        pscap = dbf.findByUuid(ps.getUuid(), PrimaryStorageCapacityVO.class);
        long volumeSize = Math.round((pscap.getAvailableCapacity() * MevocoGlobalConfig.PRIMARY_STORAGE_OVER_PROVISIONING_RATIO.value(Double.class)) / num);

        DiskOfferingInventory dio = new DiskOfferingInventory();
        dio.setDiskSize(volumeSize);
        dio.setName("data");
        dio = api.addDiskOfferingByFullConfig(dio);

        for (int i=0; i<num-1; i++) {
            VolumeInventory vol = api.createDataVolume("data", dio.getUuid());
            api.attachVolumeToVm(vm.getUuid(), vol.getUuid());
        }

        used = Math.round(usedVolumeSize() / MevocoGlobalConfig.PRIMARY_STORAGE_OVER_PROVISIONING_RATIO.value(Double.class));
        pscap = dbf.findByUuid(ps.getUuid(), PrimaryStorageCapacityVO.class);
        long errorMargin = pscap.getTotalCapacity() - used - pscap.getAvailableCapacity();
        Assert.assertTrue(errorMargin >= -10 && errorMargin <= 10);

        ref = dbf.findByUuid(host.getUuid(), LocalStorageHostRefVO.class);
        errorMargin = ref.getTotalCapacity() - used - ref.getAvailableCapacity();
        Assert.assertTrue(errorMargin >= -10 && errorMargin <= 10);

        boolean s = false;
        VolumeInventory vol = api.createDataVolume("data", dio.getUuid());
        try {
            api.attachVolumeToVm(vm.getUuid(), vol.getUuid());
        } catch (ApiSenderException e) {
            s = true;
        }
        Assert.assertTrue(s);

        MevocoGlobalConfig.PRIMARY_STORAGE_OVER_PROVISIONING_RATIO.updateValue(1);
        TimeUnit.SECONDS.sleep(2);

        used = Math.round(usedVolumeSize() / MevocoGlobalConfig.PRIMARY_STORAGE_OVER_PROVISIONING_RATIO.value(Double.class));
        pscap = dbf.findByUuid(ps.getUuid(), PrimaryStorageCapacityVO.class);
        errorMargin = pscap.getTotalCapacity() - used - pscap.getAvailableCapacity();
        Assert.assertTrue(errorMargin >= -10 && errorMargin <= 10);

        ref = dbf.findByUuid(host.getUuid(), LocalStorageHostRefVO.class);
        errorMargin = ref.getTotalCapacity() - used - ref.getAvailableCapacity();
        Assert.assertTrue(errorMargin >= -10 && errorMargin <= 10);

        VmInstanceVO vmvo = dbf.findByUuid(vm.getUuid(), VmInstanceVO.class);
        for (VolumeVO v : vmvo.getAllVolumes()) {
            if (v.getType() == VolumeType.Data) {
                api.deleteDataVolume(v.getUuid());
            }
        }

        used = usedVolumeSize();

        pscap = dbf.findByUuid(ps.getUuid(), PrimaryStorageCapacityVO.class);
        errorMargin = pscap.getTotalCapacity() - used - pscap.getAvailableCapacity();
        Assert.assertTrue(errorMargin >= -10 && errorMargin <= 10);

        ref = dbf.findByUuid(host.getUuid(), LocalStorageHostRefVO.class);
        errorMargin = ref.getTotalCapacity() - used - ref.getAvailableCapacity();
        Assert.assertTrue(errorMargin >= -10 && errorMargin <= 10);
    }
}
