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
import org.zstack.storage.ceph.backup.CephBackupStorageMonVO_;
import org.zstack.storage.ceph.backup.CephBackupStorageSimulatorConfig;
import org.zstack.storage.ceph.primary.CephPrimaryStorageMonVO;
import org.zstack.storage.ceph.primary.CephPrimaryStorageSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;

/**
 * 1. use ceph for backup storage and primary storage
 * 2. Update the primary storage information
 * 3. confirm information are updated
 */
public class TestUpdateCephPrimaryStorageMon {
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
        SimpleQuery<CephPrimaryStorageMonVO> q = dbf.createQuery(CephPrimaryStorageMonVO.class);
        q.add(CephBackupStorageMonVO_.hostname, SimpleQuery.Op.EQ, "127.0.0.1");
        CephPrimaryStorageMonVO psmon = q.find();

        Assert.assertEquals("root", psmon.getSshUsername());
        Assert.assertEquals("password", psmon.getSshPassword());
        Assert.assertEquals(22, psmon.getSshPort());

        psmon.setSshPort(20222);
        psmon.setMonPort(6789);
        psmon.setHostname("updatehost");
        psmon.setSshUsername("updateuser");
        psmon.setSshPassword("updatepassword");
        api.updateCephPrimaryStorageMon(psmon);
        psmon = dbf.reload(psmon);

        Assert.assertEquals(20222, psmon.getSshPort());
        Assert.assertEquals(6789, psmon.getMonPort());
        Assert.assertEquals("updatehost", psmon.getHostname());
        Assert.assertEquals("updateuser", psmon.getSshUsername());
        Assert.assertEquals("updatepassword", psmon.getSshPassword());
    }
}
