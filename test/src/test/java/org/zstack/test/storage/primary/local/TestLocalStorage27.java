package org.zstack.test.storage.primary.local;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.host.HostInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.storage.primary.*;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeStatus;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.storage.primary.local.LocalStorageHostRefVO;
import org.zstack.storage.primary.local.LocalStorageHostRefVOFinder;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig.Capacity;
import org.zstack.test.*;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.data.SizeUnit;

import java.util.List;

/**
 * 1. use local storage
 * 2. create a vm
 * 3. set image cache check to false
 * 4. create another vm
 * <p>
 * confirm the image is re-downloaded
 * confirm the image cache has only image
 * confirm the capacity is correct
 */
public class TestLocalStorage27 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    LocalStorageSimulatorConfig config;
    long totalSize = SizeUnit.GIGABYTE.toByte(100);
    PrimaryStorageOverProvisioningManager ratioMgr;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/localStorage/TestLocalStorage1.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("localStorageSimulator.xml");
        deployer.addSpringConfig("localStorage.xml");
        deployer.load();

        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(LocalStorageSimulatorConfig.class);
        ratioMgr = loader.getComponent(PrimaryStorageOverProvisioningManager.class);

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
            used += ratioMgr.calculateByRatio(v.getPrimaryStorageUuid(), v.getSize());
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
    public void test() throws ApiSenderException {
        InstanceOfferingInventory ioinv = deployer.instanceOfferings.get("TestInstanceOffering");
        ImageInventory img = deployer.images.get("TestImage");
        L3NetworkInventory l3 = deployer.l3Networks.get("TestL3Network1");
        VmInstanceInventory vm1 = deployer.vms.get("TestVm");

        config.downloadBitsCmds.clear();
        config.checkBitsSuccess = false;
        VmCreator creator = new VmCreator(api);
        creator.imageUuid = img.getUuid();
        creator.name = "vm";
        creator.instanceOfferingUuid = ioinv.getUuid();
        creator.addL3Network(l3.getUuid());
        VmInstanceInventory vm2 = creator.create();

        Assert.assertFalse(config.downloadBitsCmds.isEmpty());
        long count = dbf.count(ImageCacheVO.class);
        // two local storage ps
        long imageCacheNum = 2;
        if (vm1.getRootVolume().getPrimaryStorageUuid().equals(vm2.getRootVolume().getPrimaryStorageUuid())) {
            imageCacheNum = 1;
        }
        Assert.assertEquals(imageCacheNum, count);

        long used = usedVolumeSize();
        PrimaryStorageInventory local = deployer.primaryStorages.get("local");
        PrimaryStorageInventory local2 = deployer.primaryStorages.get("local2");
        PrimaryStorageCapacityVO pscap = dbf.findByUuid(local.getUuid(), PrimaryStorageCapacityVO.class);
        Assert.assertEquals(pscap.getTotalCapacity() - used, pscap.getAvailableCapacity());

        HostInventory host = deployer.hosts.get("host1");

        LocalStorageHostRefVO ref = new LocalStorageHostRefVOFinder().findByPrimaryKey(host.getUuid(), local.getUuid());
        Assert.assertEquals(ref.getTotalCapacity() - used, ref.getAvailableCapacity());
    }
}
