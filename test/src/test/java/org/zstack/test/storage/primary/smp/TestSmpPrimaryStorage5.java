package org.zstack.test.storage.primary.smp;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.storage.primary.PrimaryStorageCapacityVO;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.storage.primary.smp.SMPPrimaryStorageSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.data.SizeUnit;

/**
 * 1. use smp storage
 * 2. resize the primary storage
 * <p>
 * confirm resize success
 */
public class TestSmpPrimaryStorage5 {
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
        deployer = new Deployer("deployerXml/smpPrimaryStorage/TestSmpPrimaryStorage.xml", con);
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
        PrimaryStorageInventory smp = deployer.primaryStorages.get("smp");

        config.totalCapacity = SizeUnit.TERABYTE.toByte(10);
        config.availableCapcacity = SizeUnit.TERABYTE.toByte(5);

        PrimaryStorageCapacityVO cap = dbf.findByUuid(smp.getUuid(), PrimaryStorageCapacityVO.class);
        cap.setAvailableCapacity(0);
        cap.setTotalCapacity(0);
        dbf.update(cap);

        api.reconnectPrimaryStorage(smp.getUuid());

        cap = dbf.findByUuid(smp.getUuid(), PrimaryStorageCapacityVO.class);
        Assert.assertEquals(config.totalCapacity, cap.getTotalCapacity());
        Assert.assertEquals(config.totalCapacity, cap.getTotalPhysicalCapacity());
        Assert.assertEquals(config.availableCapcacity, cap.getAvailablePhysicalCapacity());
    }
}
