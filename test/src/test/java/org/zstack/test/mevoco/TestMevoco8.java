package org.zstack.test.mevoco;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.host.HostInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.storage.primary.*;
import org.zstack.header.volume.VolumeStatus;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
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
 * 1. set primary storage over-provisioning ratio to 1
 * 2. create a vm
 * 3. record current available capacity as cap1
 * 4. change ratio to 2, record current available capacity as cap2
 * 5. change ratio back to 1
 * <p>
 * confirm cap1 = current available capacity
 * <p>
 * 6. change ratio to 2
 * <p>
 * confirm cap2 = current available capacity
 */
public class TestMevoco8 {
    CLogger logger = Utils.getLogger(TestMevoco8.class);
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
        MevocoGlobalConfig.PRIMARY_STORAGE_OVER_PROVISIONING_RATIO.updateValue(1);
        TimeUnit.SECONDS.sleep(2);

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
        api.setTimeout(10000);
        PrimaryStorageInventory local = deployer.primaryStorages.get("local");
        HostInventory host = deployer.hosts.get("host1");

        PrimaryStorageCapacityVO pscap = dbf.findByUuid(local.getUuid(), PrimaryStorageCapacityVO.class);
        LocalStorageHostRefVO href = new LocalStorageHostRefVOFinder().findByPrimaryKey(host.getUuid(), local.getUuid());
        Assert.assertEquals(pscap.getTotalCapacity() - usedVolumeSize(), pscap.getAvailableCapacity());
        Assert.assertEquals(href.getTotalCapacity() - usedVolumeSize(), href.getAvailableCapacity());
        long psa = pscap.getAvailableCapacity();
        long hcap = href.getAvailableCapacity();

        double origin = MevocoGlobalConfig.PRIMARY_STORAGE_OVER_PROVISIONING_RATIO.value(Double.class);
        MevocoGlobalConfig.PRIMARY_STORAGE_OVER_PROVISIONING_RATIO.updateValue(2);
        TimeUnit.SECONDS.sleep(2);
        pscap = dbf.findByUuid(local.getUuid(), PrimaryStorageCapacityVO.class);
        href = new LocalStorageHostRefVOFinder().findByPrimaryKey(host.getUuid(), local.getUuid());
        Assert.assertEquals(pscap.getTotalCapacity() - usedVolumeSize(), pscap.getAvailableCapacity());
        Assert.assertEquals(href.getTotalCapacity() - usedVolumeSize(), href.getAvailableCapacity());

        long psa1 = pscap.getAvailableCapacity();
        long hcap1 = href.getAvailableCapacity();

        MevocoGlobalConfig.PRIMARY_STORAGE_OVER_PROVISIONING_RATIO.updateValue(origin);
        TimeUnit.SECONDS.sleep(2);
        pscap = dbf.findByUuid(local.getUuid(), PrimaryStorageCapacityVO.class);
        href = new LocalStorageHostRefVOFinder().findByPrimaryKey(host.getUuid(), local.getUuid());
        Assert.assertEquals(pscap.getTotalCapacity() - usedVolumeSize(), pscap.getAvailableCapacity());
        Assert.assertEquals(href.getTotalCapacity() - usedVolumeSize(), href.getAvailableCapacity());

        Assert.assertEquals(psa, pscap.getAvailableCapacity());
        Assert.assertEquals(hcap, href.getAvailableCapacity());

        MevocoGlobalConfig.PRIMARY_STORAGE_OVER_PROVISIONING_RATIO.updateValue(2);
        TimeUnit.SECONDS.sleep(2);
        pscap = dbf.findByUuid(local.getUuid(), PrimaryStorageCapacityVO.class);
        href = new LocalStorageHostRefVOFinder().findByPrimaryKey(host.getUuid(), local.getUuid());
        Assert.assertEquals(pscap.getTotalCapacity() - usedVolumeSize(), pscap.getAvailableCapacity());
        Assert.assertEquals(href.getTotalCapacity() - usedVolumeSize(), href.getAvailableCapacity());

        Assert.assertEquals(psa1, pscap.getAvailableCapacity());
        Assert.assertEquals(hcap1, href.getAvailableCapacity());

    }
}
