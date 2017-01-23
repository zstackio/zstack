package org.zstack.test.storage.primary.local;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.host.APIAddHostEvent;
import org.zstack.header.host.HostInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.kvm.APIAddKVMHostMsg;
import org.zstack.storage.primary.local.LocalStorageHostRefVOFinder;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig.Capacity;
import org.zstack.test.*;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.data.SizeUnit;

import java.util.concurrent.TimeUnit;

/**
 * 1. use local storage
 * 2. add 2 hosts
 * 3. delete one host
 * <p>
 * confirm the local storage capacity reduced to a half
 * <p>
 * 4. delete another host
 * <p>
 * confirm the local storage capacity is zero
 */
public class TestLocalStorage7 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    LocalStorageSimulatorConfig config;
    long totalSize = SizeUnit.GIGABYTE.toByte(100);

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/localStorage/TestLocalStorage6.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("localStorageSimulator.xml");
        deployer.addSpringConfig("localStorage.xml");
        deployer.load();

        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(LocalStorageSimulatorConfig.class);

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
        ClusterInventory cluster = deployer.clusters.get("Cluster1");

        // add host 1
        APIAddKVMHostMsg msg = new APIAddKVMHostMsg();
        msg.setName("host1");
        msg.setClusterUuid(cluster.getUuid());
        msg.setManagementIp("127.0.0.1");
        msg.setSession(api.getAdminSession());
        msg.setUsername("root");
        msg.setPassword("password");
        ApiSender sender = api.getApiSender();
        APIAddHostEvent evt = sender.send(msg, APIAddHostEvent.class);
        HostInventory host1 = evt.getInventory();

        // add host 2
        msg = new APIAddKVMHostMsg();
        msg.setName("host2");
        msg.setClusterUuid(cluster.getUuid());
        msg.setManagementIp("localhost");
        msg.setSession(api.getAdminSession());
        msg.setUsername("root");
        msg.setPassword("password");
        sender = api.getApiSender();
        evt = sender.send(msg, APIAddHostEvent.class);
        HostInventory host2 = evt.getInventory();

        // check
        TimeUnit.SECONDS.sleep(5);
        PrimaryStorageInventory local1 = deployer.primaryStorages.get("local");
        PrimaryStorageInventory local2 = deployer.primaryStorages.get("local2");

        {
            PrimaryStorageVO lvo1 = dbf.findByUuid(local1.getUuid(), PrimaryStorageVO.class);
            Assert.assertEquals(totalSize * 2, lvo1.getCapacity().getTotalCapacity());
            Assert.assertEquals(totalSize * 2, lvo1.getCapacity().getAvailableCapacity());
            Assert.assertEquals(totalSize * 2, lvo1.getCapacity().getTotalPhysicalCapacity());
            Assert.assertEquals(totalSize * 2, lvo1.getCapacity().getAvailablePhysicalCapacity());
        }


        {
            Assert.assertTrue(new LocalStorageHostRefVOFinder().isExist(host1.getUuid(), local1.getUuid()));
            Assert.assertTrue(new LocalStorageHostRefVOFinder().isExist(host1.getUuid(), local2.getUuid()));

            Assert.assertTrue(new LocalStorageHostRefVOFinder().isExist(host2.getUuid(), local1.getUuid()));
            Assert.assertTrue(new LocalStorageHostRefVOFinder().isExist(host2.getUuid(), local2.getUuid()));
        }
        api.deleteHost(host1.getUuid());
        TimeUnit.SECONDS.sleep(5);
        {
            Assert.assertFalse(new LocalStorageHostRefVOFinder().isExist(host1.getUuid(), local1.getUuid()));
            Assert.assertFalse(new LocalStorageHostRefVOFinder().isExist(host1.getUuid(), local2.getUuid()));

            Assert.assertTrue(new LocalStorageHostRefVOFinder().isExist(host2.getUuid(), local1.getUuid()));
            Assert.assertTrue(new LocalStorageHostRefVOFinder().isExist(host2.getUuid(), local2.getUuid()));

            {
                PrimaryStorageVO lvo1 = dbf.findByUuid(local1.getUuid(), PrimaryStorageVO.class);
                Assert.assertEquals(totalSize, lvo1.getCapacity().getTotalCapacity());
                Assert.assertEquals(totalSize, lvo1.getCapacity().getAvailableCapacity());
                Assert.assertEquals(totalSize, lvo1.getCapacity().getTotalPhysicalCapacity());
                Assert.assertEquals(totalSize, lvo1.getCapacity().getAvailablePhysicalCapacity());
            }
            {
                PrimaryStorageVO lvo2 = dbf.findByUuid(local2.getUuid(), PrimaryStorageVO.class);
                Assert.assertEquals(totalSize, lvo2.getCapacity().getTotalCapacity());
                Assert.assertEquals(totalSize, lvo2.getCapacity().getAvailableCapacity());
                Assert.assertEquals(totalSize, lvo2.getCapacity().getTotalPhysicalCapacity());
                Assert.assertEquals(totalSize, lvo2.getCapacity().getAvailablePhysicalCapacity());
            }
        }
        api.deleteHost(host2.getUuid());
        TimeUnit.SECONDS.sleep(5);
        {
            Assert.assertFalse(new LocalStorageHostRefVOFinder().isExist(host2.getUuid(), local1.getUuid()));
            Assert.assertFalse(new LocalStorageHostRefVOFinder().isExist(host2.getUuid(), local2.getUuid()));
            {
                PrimaryStorageVO lvo1 = dbf.findByUuid(local1.getUuid(), PrimaryStorageVO.class);
                Assert.assertEquals(0, lvo1.getCapacity().getTotalCapacity());
                Assert.assertEquals(0, lvo1.getCapacity().getAvailableCapacity());
                Assert.assertEquals(0, lvo1.getCapacity().getTotalPhysicalCapacity());
                Assert.assertEquals(0, lvo1.getCapacity().getAvailablePhysicalCapacity());
            }
            {
                PrimaryStorageVO lvo2 = dbf.findByUuid(local2.getUuid(), PrimaryStorageVO.class);
                Assert.assertEquals(0, lvo2.getCapacity().getTotalCapacity());
                Assert.assertEquals(0, lvo2.getCapacity().getAvailableCapacity());
                Assert.assertEquals(0, lvo2.getCapacity().getTotalPhysicalCapacity());
                Assert.assertEquals(0, lvo2.getCapacity().getAvailablePhysicalCapacity());
            }
        }
    }
}
