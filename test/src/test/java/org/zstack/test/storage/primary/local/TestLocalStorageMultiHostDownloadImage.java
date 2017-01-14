package org.zstack.test.storage.primary.local;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.host.HostInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.storage.primary.local.LocalStorageHostRefVO;
import org.zstack.storage.primary.local.LocalStorageHostRefVOFinder;
import org.zstack.storage.primary.local.LocalStorageResourceRefVO;
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

import static org.zstack.utils.CollectionDSL.list;

/**
 * 1. use local storage
 * 2. use two host
 * 3. via creating vm, download one iso to one host, download the same iso to the other host, ensure success
 * 4. make sure capacity right all the way
 */
public class TestLocalStorageMultiHostDownloadImage {
    CLogger logger = Utils.getLogger(TestLocalStorageMultiHostDownloadImage.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    LocalStorageSimulatorConfig localStorageSimulatorConfig;
    long totalSize = SizeUnit.GIGABYTE.toByte(100);

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/localStorage/TestLocalStorageMultiHostDownloadImage.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("localStorageSimulator.xml");
        deployer.addSpringConfig("localStorage.xml");
        deployer.load();

        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        localStorageSimulatorConfig = loader.getComponent(LocalStorageSimulatorConfig.class);

        Capacity c = new Capacity();
        c.total = totalSize;
        c.avail = totalSize;

        localStorageSimulatorConfig.capacityMap.put("host1", c);
        localStorageSimulatorConfig.capacityMap.put("host2", c);

        deployer.build();
        api = deployer.getApi();
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException {
        HostInventory host = deployer.hosts.get("host1");
        HostInventory host2 = deployer.hosts.get("host2");
        PrimaryStorageInventory local = deployer.primaryStorages.get("local");
        PrimaryStorageInventory local2 = deployer.primaryStorages.get("local2");
        ImageInventory image = deployer.images.get("TestImage");
        ImageInventory iso = deployer.images.get("TestIso");
        ClusterInventory cluster = deployer.clusters.get("Cluster1");
        L3NetworkInventory l3network = deployer.l3Networks.get("TestL3Network1");
        DiskOfferingInventory diskOffering = deployer.diskOfferings.get("TestDiskOffering1");
        InstanceOfferingInventory instanceOffering = deployer.instanceOfferings.get("TestInstanceOffering");

        {
            LocalStorageHostRefVO href = new LocalStorageHostRefVOFinder()
                    .findByPrimaryKey(host.getUuid(), local.getUuid());
            Assert.assertEquals(href.getTotalCapacity(), totalSize);
            Assert.assertEquals(href.getTotalPhysicalCapacity(), totalSize);
            LocalStorageHostRefVO href2 = new LocalStorageHostRefVOFinder()
                    .findByPrimaryKey(host2.getUuid(), local.getUuid());
            Assert.assertEquals(href2.getTotalCapacity(), totalSize);
            Assert.assertEquals(href2.getTotalPhysicalCapacity(), totalSize);
        }
        {
            LocalStorageHostRefVO href = new LocalStorageHostRefVOFinder()
                    .findByPrimaryKey(host.getUuid(), local2.getUuid());
            Assert.assertEquals(href.getTotalCapacity(), totalSize);
            Assert.assertEquals(href.getTotalPhysicalCapacity(), totalSize);
            LocalStorageHostRefVO href2 = new LocalStorageHostRefVOFinder()
                    .findByPrimaryKey(host2.getUuid(), local2.getUuid());
            Assert.assertEquals(href2.getTotalCapacity(), totalSize);
            Assert.assertEquals(href2.getTotalPhysicalCapacity(), totalSize);
        }

        Assert.assertTrue(!localStorageSimulatorConfig.initCmdList.isEmpty());
        Assert.assertTrue(localStorageSimulatorConfig.downloadBitsCmds.isEmpty());

        // create a new vm on host 1
        {
            VmInstanceInventory testVm = new VmInstanceInventory();
            testVm.setName("testVm");
            testVm.setImageUuid(iso.getUuid());
            testVm.setUuid(null);
            testVm.setZoneUuid(null);
            testVm.setClusterUuid(cluster.getUuid());
            testVm.setHostUuid(host.getUuid());
            testVm.setDefaultL3NetworkUuid(l3network.getUuid());
            testVm.setInstanceOfferingUuid(instanceOffering.getUuid());
            testVm = api.createVmByFullConfigWithSpecifiedPS(
                    testVm,
                    diskOffering.getUuid(),
                    list(l3network.getUuid()),
                    null,
                    null,
                    local.getUuid(),
                    session
            );
            Assert.assertEquals(VmInstanceState.Running.toString(), testVm.getState());
            // Assert.assertEquals(host.getUuid(), testVm.getHostUuid());
            logger.debug(String.format("vm[uuid:%s] is on host[uuid:%s]", testVm.getUuid(), testVm.getUuid()));
        }

        // create a new vm on host 2
        {
            VmInstanceInventory testVm2 = new VmInstanceInventory();
            testVm2.setName("testVm2");
            testVm2.setImageUuid(iso.getUuid());
            testVm2.setUuid(null);
            testVm2.setZoneUuid(null);
            testVm2.setClusterUuid(cluster.getUuid());
            testVm2.setHostUuid(host2.getUuid());
            testVm2.setDefaultL3NetworkUuid(l3network.getUuid());
            testVm2.setInstanceOfferingUuid(instanceOffering.getUuid());
            testVm2 = api.createVmByFullConfigWithSpecifiedPS(
                    testVm2,
                    diskOffering.getUuid(),
                    list(l3network.getUuid()),
                    null,
                    null,
                    local.getUuid(),
                    session
            );
            Assert.assertEquals(VmInstanceState.Running.toString(), testVm2.getState());
            // Assert.assertEquals(host.getUuid(), testVm2.getHostUuid());
            logger.debug(String.format("vm[uuid:%s] is on host[uuid:%s]", testVm2.getUuid(), testVm2.getUuid()));
        }

        //
        Assert.assertTrue(localStorageSimulatorConfig.downloadBitsCmds.size() == 2);
        Assert.assertTrue(Q.New(LocalStorageResourceRefVO.class).count() == 2 + 2);
    }
}
