package org.zstack.test.storage.primary.local;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.host.HostInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.storage.primary.ImageCacheVO;
import org.zstack.header.storage.primary.PrimaryStorageCapacityVO;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.storage.primary.PrimaryStorageOverProvisioningManager;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeVO;
import org.zstack.storage.primary.local.LocalStorageHostRefVO;
import org.zstack.storage.primary.local.LocalStorageHostRefVOFinder;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig.Capacity;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.data.SizeUnit;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 1. use local storage
 * 2. create a vm
 * <p>
 * confirm all local storage related commands, VOs are set
 * <p>
 * 3. attach another local storage to the cluster
 * <p>
 * confirm unable to attach
 * <p>
 * 4. add a new host which doesn't have any image cache
 * 5. reconnect the host
 * <p>
 * confirm reconnect successfully
 * <p>
 * 6. remove a host record of the local storage from database
 * 7. reconnect the primary storage
 * <p>
 * confirm the capacity re-calculated
 */
public class TestLocalStorage44 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    LocalStorageSimulatorConfig config;
    PrimaryStorageOverProvisioningManager ratioMgr;
    long totalSize = SizeUnit.GIGABYTE.toByte(100);

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

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        HostInventory host1 = deployer.hosts.get("host1");
        PrimaryStorageInventory local = deployer.primaryStorages.get("local");
        PrimaryStorageInventory local2 = deployer.primaryStorages.get("local2");
        VmInstanceInventory vm = deployer.vms.get("TestVm");

        LocalStorageHostRefVO href = new LocalStorageHostRefVOFinder()
                .findByPrimaryKey(host1.getUuid(), vm.getRootVolume().getPrimaryStorageUuid());
        Assert.assertEquals(href.getTotalCapacity(), totalSize);
        Assert.assertEquals(href.getTotalPhysicalCapacity(), totalSize);


        long usedSize = 0;
        {
            List<ImageCacheVO> imgs = dbf.listAll(ImageCacheVO.class);
            for (ImageCacheVO i : imgs) {
                usedSize += i.getSize();
            }
            List<VolumeVO> vols = dbf.listAll(VolumeVO.class);
            for (VolumeVO v : vols) {
                usedSize += ratioMgr.calculateByRatio(vm.getRootVolume().getPrimaryStorageUuid(), v.getSize());
            }
        }

        PrimaryStorageCapacityVO pscap;
        Capacity c;
        // expand the the host storage capacity local storage
        {
            totalSize = SizeUnit.GIGABYTE.toByte(599);
            c = new Capacity();
            c.total = totalSize;
            c.avail = totalSize;

            config.capacityMap.put("host1", c);
            api.reconnectHost(host1.getUuid());
            TimeUnit.SECONDS.sleep(6);

            href = new LocalStorageHostRefVOFinder().findByPrimaryKey(host1.getUuid(), vm.getRootVolume().getPrimaryStorageUuid());
            Assert.assertEquals(totalSize, href.getTotalCapacity());
            Assert.assertEquals(totalSize, href.getAvailablePhysicalCapacity());
            Assert.assertEquals(totalSize, href.getTotalPhysicalCapacity());
            Assert.assertEquals(totalSize - usedSize, href.getAvailableCapacity());

            pscap = dbf.findByUuid(vm.getRootVolume().getPrimaryStorageUuid(), PrimaryStorageCapacityVO.class);
            Assert.assertEquals(totalSize, pscap.getTotalCapacity());
            Assert.assertEquals(totalSize, pscap.getTotalPhysicalCapacity());
            Assert.assertEquals(totalSize, pscap.getAvailablePhysicalCapacity());
            Assert.assertEquals(totalSize - usedSize, pscap.getAvailableCapacity());
        }
        // squeeze the the local storage
        {
            totalSize = SizeUnit.GIGABYTE.toByte(80);
            c = new Capacity();
            c.total = totalSize;
            c.avail = totalSize;

            config.capacityMap.put("host1", c);
            api.reconnectHost(host1.getUuid());

            TimeUnit.SECONDS.sleep(3);

            href = new LocalStorageHostRefVOFinder().findByPrimaryKey(host1.getUuid(), vm.getRootVolume().getPrimaryStorageUuid());
            Assert.assertEquals(totalSize, href.getTotalCapacity());
            Assert.assertEquals(totalSize, href.getAvailablePhysicalCapacity());
            Assert.assertEquals(totalSize, href.getTotalPhysicalCapacity());
            Assert.assertEquals(totalSize - usedSize, href.getAvailableCapacity());

            pscap = dbf.findByUuid(vm.getRootVolume().getPrimaryStorageUuid(), PrimaryStorageCapacityVO.class);
            Assert.assertEquals(totalSize, pscap.getTotalCapacity());
            Assert.assertEquals(totalSize, pscap.getTotalPhysicalCapacity());
            Assert.assertEquals(totalSize, pscap.getAvailablePhysicalCapacity());
            Assert.assertEquals(totalSize - usedSize, pscap.getAvailableCapacity());
        }
        // add another host
        {
            c = new Capacity();
            c.total = totalSize;
            c.avail = totalSize;

            config.capacityMap.put("host2", c);
            HostInventory host2 = api.addKvmHost("host2", "127.0.0.1", host1.getClusterUuid());
            api.reconnectHost(host2.getUuid());

            LocalStorageHostRefVO refHost2 = new LocalStorageHostRefVOFinder().findByPrimaryKey(host2.getUuid(), vm.getRootVolume().getPrimaryStorageUuid());
            // make available capacity bigger than total capacity
            // to simulate abnormal conditions.
            pscap = dbf.findByUuid(vm.getRootVolume().getPrimaryStorageUuid(), PrimaryStorageCapacityVO.class);
            pscap.setAvailableCapacity(pscap.getTotalCapacity() + SizeUnit.GIGABYTE.toByte(10));
            dbf.update(pscap);
            dbf.remove(refHost2);

            // reconnect should be able to correct the capacity
            api.reconnectPrimaryStorage(vm.getRootVolume().getPrimaryStorageUuid());
            TimeUnit.SECONDS.sleep(2);
            PrimaryStorageCapacityVO pscap1 = dbf.findByUuid(vm.getRootVolume().getPrimaryStorageUuid(), PrimaryStorageCapacityVO.class);
            Assert.assertTrue(pscap1.getTotalCapacity() < pscap.getTotalCapacity());
            Assert.assertTrue(pscap1.getAvailableCapacity() <= pscap1.getTotalCapacity());
        }
    }
}
