package org.zstack.test.storage.ceph;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.storage.primary.APIAddPrimaryStorageEvent;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.storage.ceph.primary.APIAddCephPrimaryStorageMsg;
import org.zstack.storage.ceph.primary.CephPrimaryStorageMonVO;
import org.zstack.storage.ceph.primary.CephPrimaryStorageSimulatorConfig;
import org.zstack.test.*;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.SizeUtils;

import static org.zstack.utils.CollectionDSL.list;

/**
 * 1. add a ceph primary storage with two mons
 * 2. make the mon failed to be connected
 * <p>
 * confirm adding the primary storage fails and the mons are removed from database
 */
public class TestCeph9 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    CephPrimaryStorageSimulatorConfig config;
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
        config = loader.getComponent(CephPrimaryStorageSimulatorConfig.class);
        kconfig = loader.getComponent(KVMSimulatorConfig.class);
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException {
        CephPrimaryStorageSimulatorConfig.CephPrimaryStorageConfig sc = new CephPrimaryStorageSimulatorConfig.CephPrimaryStorageConfig();
        sc.fsid = "7ff218d9-f525-435f-8a40-3618d1772a64";
        sc.totalCapacity = SizeUtils.sizeStringToBytes("100G");
        sc.availCapacity = SizeUtils.sizeStringToBytes("100G");
        config.config.put("ceph", sc);
        config.monInitSuccess = false;

        ZoneInventory zone = deployer.zones.get("Zone1");
        APIAddCephPrimaryStorageMsg cmsg = new APIAddCephPrimaryStorageMsg();
        cmsg.setName("ceph");
        cmsg.setMonUrls(list("root:password@localhost/?monPort=7777", "root:password@127.0.0.1/?monPort=7777"));
        cmsg.setZoneUuid(zone.getUuid());
        cmsg.setSession(api.getAdminSession());
        ApiSender sender = new ApiSender();

        boolean s = false;
        try {
            sender.send(cmsg, APIAddPrimaryStorageEvent.class);
        } catch (ApiSenderException e) {
            s = true;
        }

        Assert.assertTrue(s);
        long count = dbf.count(CephPrimaryStorageMonVO.class);

        Assert.assertEquals(0, count);
    }
}
