package org.zstack.test.storage.primary.smp;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.host.*;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.storage.primary.PrimaryStorageCapacityVO;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.storage.primary.smp.SMPPrimaryStorageFactory;
import org.zstack.storage.primary.smp.SMPPrimaryStorageSimulatorConfig;
import org.zstack.test.*;
import org.zstack.test.deployer.Deployer;

/**
 * 1. use smp storage attached to an empty cluster
 * 2. add a host
 * <p>
 * confirm smp storage's size is set
 */
public class TestSmpPrimaryStorage7 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    SMPPrimaryStorageSimulatorConfig config;
    KVMSimulatorConfig kconfig;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/smpPrimaryStorage/TestSmpPrimaryStorage7.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("smpPrimaryStorageSimulator.xml");
        deployer.addSpringConfig("sharedMountPointPrimaryStorage.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        kconfig = loader.getComponent(KVMSimulatorConfig.class);
        config = loader.getComponent(SMPPrimaryStorageSimulatorConfig.class);
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException {
        HostInventory hinv = deployer.hosts.get("host1");

        PrimaryStorageInventory smp = deployer.primaryStorages.get("smp");
        PrimaryStorageCapacityVO cap = dbf.findByUuid(smp.getUuid(), PrimaryStorageCapacityVO.class);

        api.deleteHost(hinv.getUuid());
        cap = dbf.reload(cap);
        Assert.assertEquals(0L, cap.getTotalCapacity());
        Assert.assertEquals(0L, cap.getTotalPhysicalCapacity());
        Assert.assertEquals(0L, cap.getAvailablePhysicalCapacity());
    }
}
