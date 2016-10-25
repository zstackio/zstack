package org.zstack.test.storage.primary.local;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.host.APIAddHostEvent;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.kvm.APIAddKVMHostMsg;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig.Capacity;
import org.zstack.test.*;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.data.SizeUnit;

import java.util.concurrent.TimeUnit;

/**
 * 1. use local storage
 * 2. make adding KVM host fail,  add another host
 * <p>
 * confirm the local storage capacity not changed
 */
public class TestLocalStorage47 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    LocalStorageSimulatorConfig config;
    KVMSimulatorConfig kconfig;
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
        kconfig = loader.getComponent(KVMSimulatorConfig.class);

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
        PrimaryStorageInventory local = deployer.primaryStorages.get("local");
        PrimaryStorageInventory local2 = deployer.primaryStorages.get("local2");
        ClusterInventory cluster = deployer.clusters.get("Cluster1");
        PrimaryStorageVO psvo = dbf.findByUuid(local.getUuid(), PrimaryStorageVO.class);
        Assert.assertEquals(totalSize, psvo.getCapacity().getTotalCapacity());

        kconfig.checkPhysicalInterfaceSuccess = false;
        APIAddKVMHostMsg msg = new APIAddKVMHostMsg();
        msg.setName("host2");
        msg.setClusterUuid(cluster.getUuid());
        msg.setManagementIp("127.0.0.1");
        msg.setSession(api.getAdminSession());
        msg.setUsername("root");
        msg.setPassword("password");
        ApiSender sender = api.getApiSender();

        boolean s = false;
        try {
            sender.send(msg, APIAddHostEvent.class);
        } catch (ApiSenderException e) {
            s = true;
        }
        Assert.assertTrue(s);

        TimeUnit.SECONDS.sleep(10);

        PrimaryStorageVO lvo = dbf.findByUuid(local.getUuid(), PrimaryStorageVO.class);
        Assert.assertEquals(psvo.getCapacity().getTotalCapacity(), lvo.getCapacity().getTotalCapacity());
        Assert.assertEquals(psvo.getCapacity().getAvailableCapacity(), lvo.getCapacity().getAvailableCapacity());
        Assert.assertEquals(psvo.getCapacity().getTotalPhysicalCapacity(), lvo.getCapacity().getTotalPhysicalCapacity());
        Assert.assertEquals(psvo.getCapacity().getAvailablePhysicalCapacity(), lvo.getCapacity().getAvailablePhysicalCapacity());
    }
}
