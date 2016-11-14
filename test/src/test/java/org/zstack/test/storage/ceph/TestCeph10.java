package org.zstack.test.storage.ceph;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.storage.backup.APIAddBackupStorageEvent;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.storage.ceph.backup.APIAddCephBackupStorageMsg;
import org.zstack.storage.ceph.backup.CephBackupStorageMonVO;
import org.zstack.storage.ceph.backup.CephBackupStorageSimulatorConfig;
import org.zstack.storage.ceph.backup.CephBackupStorageSimulatorConfig.CephBackupStorageConfig;
import org.zstack.test.*;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.SizeUtils;

import static org.zstack.utils.CollectionDSL.list;

/**
 * 1. add a ceph backup storage with two mons
 * 2. make the mon failed to be connected
 * <p>
 * confirm adding the backup storage fails and the mons are removed from database
 */
public class TestCeph10 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    CephBackupStorageSimulatorConfig config;
    KVMSimulatorConfig kconfig;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/ceph/TestCeph9.xml", con);
        deployer.addSpringConfig("ceph.xml");
        deployer.addSpringConfig("cephSimulator.xml");
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(CephBackupStorageSimulatorConfig.class);
        kconfig = loader.getComponent(KVMSimulatorConfig.class);
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException {
        CephBackupStorageConfig sc = new CephBackupStorageConfig();
        sc.fsid = "7ff218d9-f525-435f-8a40-3618d1772a64";
        sc.totalCapacity = SizeUtils.sizeStringToBytes("100G");
        sc.availCapacity = SizeUtils.sizeStringToBytes("100G");
        config.config.put("ceph", sc);
        config.monInitSuccess = false;

        APIAddCephBackupStorageMsg cmsg = new APIAddCephBackupStorageMsg();
        cmsg.setName("ceph");
        cmsg.setMonUrls(list("root:password@localhost/?monPort=7777", "root:password@127.0.0.1/?monPort=7777"));
        cmsg.setSession(api.getAdminSession());
        ApiSender sender = new ApiSender();

        boolean s = false;
        try {
            sender.send(cmsg, APIAddBackupStorageEvent.class);
        } catch (ApiSenderException e) {
            s = true;
        }

        Assert.assertTrue(s);
        long count = dbf.count(CephBackupStorageMonVO.class);

        Assert.assertEquals(0, count);
    }
}
