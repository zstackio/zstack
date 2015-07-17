package org.zstack.test.storage.primary.nfs;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.config.GlobalConfigFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.storage.primary.ImageCacheVO;
import org.zstack.header.storage.primary.ImageCacheVO_;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.simulator.storage.primary.nfs.NfsPrimaryStorageSimulatorConfig;
import org.zstack.test.*;
import org.zstack.test.deployer.Deployer;


/**
 * 1. delete a nfs primary storage
 *
 * confirm it's umounted on the kvm hosts
 */
public class TestDeleteNfsPrimaryStorage {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    NfsPrimaryStorageSimulatorConfig config;
    KVMSimulatorConfig kconfig;
    GlobalConfigFacade gcf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/primaryStorage/TestImageCacheMissing.xml", con);
        deployer.addSpringConfig("SftpBackupStorage.xml");
        deployer.addSpringConfig("SftpBackupStorageSimulator.xml");
        deployer.addSpringConfig("Kvm.xml");
        deployer.addSpringConfig("KVMSimulator.xml");
        deployer.addSpringConfig("NfsPrimaryStorage.xml");
        deployer.addSpringConfig("NfsPrimaryStorageSimulator.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        gcf = loader.getComponent(GlobalConfigFacade.class);
        config = loader.getComponent(NfsPrimaryStorageSimulatorConfig.class);
        kconfig = loader.getComponent(KVMSimulatorConfig.class);
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException {
        PrimaryStorageInventory nfs = deployer.primaryStorages.get("nfs");
        api.deletePrimaryStorage(nfs.getUuid());

        Assert.assertFalse(config.unmountCmds.isEmpty());
    }
}
