package org.zstack.test.storage.ceph;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.identity.SessionInventory;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.storage.ceph.backup.CephBackupStorageMonVO;
import org.zstack.storage.ceph.backup.CephBackupStorageMonVO_;
import org.zstack.storage.ceph.backup.CephBackupStorageSimulatorConfig;
import org.zstack.storage.ceph.primary.CephPrimaryStorageSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;

/**
 * 1. use ceph for backup storage and primary storage
 * 2. Update the backup storage information
 * 3. confirm information are updated
 */
public class TestUpdateCephBackupStorageMon {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    CephPrimaryStorageSimulatorConfig config;
    KVMSimulatorConfig kconfig;
    CephBackupStorageSimulatorConfig bconfig;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/ceph/TestUpdateCephMon.xml", con);
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
        bconfig = loader.getComponent(CephBackupStorageSimulatorConfig.class);
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException {
        SimpleQuery<CephBackupStorageMonVO> q = dbf.createQuery(CephBackupStorageMonVO.class);
        q.add(CephBackupStorageMonVO_.hostname, SimpleQuery.Op.EQ, "127.0.0.1");
        CephBackupStorageMonVO bsmon = q.find();

        Assert.assertEquals("root", bsmon.getSshUsername());
        Assert.assertEquals("pass@#$word", bsmon.getSshPassword());
        Assert.assertEquals(23, bsmon.getSshPort());

        bsmon.setSshPort(20222);
        bsmon.setMonPort(6789);
        bsmon.setHostname("updatehost");
        bsmon.setSshUsername("updateuser");
        bsmon.setSshPassword("updatepassword");
        api.updateCephBackupStorageMon(bsmon);
        bsmon = dbf.reload(bsmon);

        Assert.assertEquals(20222, bsmon.getSshPort());
        Assert.assertEquals(6789, bsmon.getMonPort());
        Assert.assertEquals("updatehost", bsmon.getHostname());
        Assert.assertEquals("updateuser", bsmon.getSshUsername());
        Assert.assertEquals("updatepassword", bsmon.getSshPassword());


    }
}
