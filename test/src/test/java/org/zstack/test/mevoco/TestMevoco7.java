package org.zstack.test.mevoco;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.compute.vm.VmGlobalConfig;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.host.HostInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.storage.primary.*;
import org.zstack.header.vm.VmInstanceDeletionPolicyManager.VmInstanceDeletionPolicy;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.volume.VolumeDeletionPolicyManager.VolumeDeletionPolicy;
import org.zstack.header.volume.*;
import org.zstack.mevoco.MevocoGlobalConfig;
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

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 1. create 2 vms
 * 2. update PRIMARY_STORAGE_OVER_PROVISIONING_RATIO to 2.5
 * <p>
 * confirm the primary storage capacity is correct
 * <p>
 * 3. calculate a volume size that 12 volumes will occupy all available capacity after over provisioning
 * 4. create and attach 12 volumes
 * <p>
 * confirm the primary storage capacity is correct
 * <p>
 * 5. create and attach 13rd volume
 * <p>
 * confirm the volume failed to be created and attached
 * <p>
 * 6. update PRIMARY_STORAGE_OVER_PROVISIONING_RATIO to 1
 * <p>
 * confirm the primary storage capacity is correct
 * <p>
 * 7. delete all previous created data volumes
 * <p>
 * confirm the primary storage capacity is correct
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
    PrimaryStorageOverProvisioningManager psRatioMgr;
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

        Capacity c = new Capacity();
        c.total = totalSize;
        c.avail = totalSize;

        config.capacityMap.put("host1", c);
        MevocoGlobalConfig.DISTRIBUTE_IMAGE.updateValue(false);

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
            used += psRatioMgr.calculateByRatio(v.getPrimaryStorageUuid(), v.getSize());
        }

        PrimaryStorageInventory ps = deployer.primaryStorages.get("local");
        SimpleQuery<ImageCacheVO> iq = dbf.createQuery(ImageCacheVO.class);
        iq.add(ImageCacheVO_.primaryStorageUuid, Op.EQ, ps.getUuid());
        List<ImageCacheVO> is = iq.list();
        for (ImageCacheVO i : is) {
            used += i.getSize();
        }

        return used;
    }


    @Test
    public void test() throws ApiSenderException, InterruptedException {
        VmGlobalConfig.VM_DELETION_POLICY.updateValue(VmInstanceDeletionPolicy.Direct.toString());
        VolumeGlobalConfig.VOLUME_DELETION_POLICY.updateValue(VolumeDeletionPolicy.Direct.toString());
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        VmInstanceInventory vm1 = deployer.vms.get("TestVm1");
        HostInventory host = deployer.hosts.get("host1");
        PrimaryStorageInventory local = deployer.primaryStorages.get("local");

        MevocoGlobalConfig.PRIMARY_STORAGE_OVER_PROVISIONING_RATIO.updateValue(2.5);
        TimeUnit.SECONDS.sleep(2);

        long used = usedVolumeSize();
        PrimaryStorageCapacityVO pscap = dbf.findByUuid(local.getUuid(), PrimaryStorageCapacityVO.class);
        Assert.assertEquals(pscap.getTotalCapacity() - used, pscap.getAvailableCapacity());

        LocalStorageHostRefVO ref = new LocalStorageHostRefVOFinder().findByPrimaryKey(host.getUuid(), local.getUuid());
        Assert.assertEquals(ref.getTotalCapacity() - used, ref.getAvailableCapacity());

        int num = 12;
        pscap = dbf.findByUuid(local.getUuid(), PrimaryStorageCapacityVO.class);
        long ncap = psRatioMgr.calculatePrimaryStorageAvailableCapacityByRatio(pscap.getUuid(), pscap.getAvailableCapacity());
        long volumeSize = Math.round((double) (ncap / num));

        DiskOfferingInventory dio = new DiskOfferingInventory();
        dio.setDiskSize(volumeSize);
        dio.setName("data");
        dio = api.addDiskOfferingByFullConfig(dio);

        for (int i = 0; i < num; i++) {
            VolumeInventory vol = api.createDataVolume("data", dio.getUuid());
            api.attachVolumeToVm(vm.getUuid(), vol.getUuid());
        }

        used = usedVolumeSize();
        pscap = dbf.findByUuid(local.getUuid(), PrimaryStorageCapacityVO.class);
        Assert.assertEquals(pscap.getTotalCapacity() - used, pscap.getAvailableCapacity());

        ref = new LocalStorageHostRefVOFinder().findByPrimaryKey(host.getUuid(), local.getUuid());
        Assert.assertEquals(ref.getTotalCapacity() - used, ref.getAvailableCapacity());

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

        used = usedVolumeSize();
        pscap = dbf.findByUuid(local.getUuid(), PrimaryStorageCapacityVO.class);
        Assert.assertEquals(pscap.getTotalCapacity() - used, pscap.getAvailableCapacity());

        ref = new LocalStorageHostRefVOFinder().findByPrimaryKey(host.getUuid(), local.getUuid());
        Assert.assertEquals(ref.getTotalCapacity() - used, ref.getAvailableCapacity());

        VmInstanceVO vmvo = dbf.findByUuid(vm.getUuid(), VmInstanceVO.class);
        for (VolumeVO v : vmvo.getAllVolumes()) {
            if (v.getType() == VolumeType.Data) {
                api.deleteDataVolume(v.getUuid());
            }
        }

        TimeUnit.SECONDS.sleep(3);
        used = usedVolumeSize();

        pscap = dbf.findByUuid(local.getUuid(), PrimaryStorageCapacityVO.class);
        Assert.assertEquals(pscap.getTotalCapacity() - used, pscap.getAvailableCapacity());

        ref = new LocalStorageHostRefVOFinder().findByPrimaryKey(host.getUuid(), local.getUuid());
        Assert.assertEquals(ref.getTotalCapacity() - used, ref.getAvailableCapacity());
    }
}
