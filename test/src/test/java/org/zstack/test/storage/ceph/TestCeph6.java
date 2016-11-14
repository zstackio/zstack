package org.zstack.test.storage.ceph;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.storage.ceph.backup.CephBackupStorageMonVO;
import org.zstack.storage.ceph.backup.CephBackupStorageMonVO_;
import org.zstack.storage.ceph.primary.CephPrimaryStorageMonVO;
import org.zstack.storage.ceph.primary.CephPrimaryStorageMonVO_;
import org.zstack.storage.ceph.primary.CephPrimaryStorageSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;

import static org.zstack.utils.CollectionDSL.list;

/**
 * 1. use ceph for backup storage and primary storage
 * 2. add other 2 mons to both backup and primary storage
 * <p>
 * confirm the mons added successfully
 */
public class TestCeph6 {
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
        deployer = new Deployer("deployerXml/ceph/TestCeph1.xml", con);
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
        PrimaryStorageInventory ps = deployer.primaryStorages.get("ceph-pri");
        BackupStorageInventory bs = deployer.backupStorages.get("ceph-bk");

        api.removeMonFromBackupStorage(bs.getUuid(), list("127.0.0.1", "localhost"));
        api.removeMonFromPrimaryStorage(ps.getUuid(), list("127.0.0.1", "localhost"));

        api.addMonToCephBackupStorage(bs.getUuid(), list("root:password@127.0.0.1:2222/?monPort=1234", "root1:password1@localhost:3322/?monPort=5678"));
        SimpleQuery<CephBackupStorageMonVO> q = dbf.createQuery(CephBackupStorageMonVO.class);
        q.add(CephBackupStorageMonVO_.hostname, Op.EQ, "127.0.0.1");
        CephBackupStorageMonVO bmon = q.find();
        Assert.assertNotNull(bmon);
        Assert.assertEquals("root", bmon.getSshUsername());
        Assert.assertEquals("password", bmon.getSshPassword());
        Assert.assertEquals(2222, bmon.getSshPort());
        Assert.assertEquals(1234, bmon.getMonPort());

        q = dbf.createQuery(CephBackupStorageMonVO.class);
        q.add(CephBackupStorageMonVO_.hostname, Op.EQ, "localhost");
        bmon = q.find();
        Assert.assertNotNull(bmon);
        Assert.assertEquals("root1", bmon.getSshUsername());
        Assert.assertEquals("password1", bmon.getSshPassword());
        Assert.assertEquals(3322, bmon.getSshPort());
        Assert.assertEquals(5678, bmon.getMonPort());

        api.addMonToCephPrimaryStorage(ps.getUuid(), list("root:password@127.0.0.1:2222/?monPort=1234", "root1:password1@localhost:3322/?monPort=5678"));
        SimpleQuery<CephPrimaryStorageMonVO> pq = dbf.createQuery(CephPrimaryStorageMonVO.class);
        pq.add(CephPrimaryStorageMonVO_.hostname, Op.EQ, "127.0.0.1");
        CephPrimaryStorageMonVO pmon = pq.find();
        Assert.assertNotNull(pmon);
        Assert.assertEquals("root", pmon.getSshUsername());
        Assert.assertEquals("password", pmon.getSshPassword());
        Assert.assertEquals(2222, pmon.getSshPort());
        Assert.assertEquals(1234, pmon.getMonPort());

        pq = dbf.createQuery(CephPrimaryStorageMonVO.class);
        pq.add(CephPrimaryStorageMonVO_.hostname, Op.EQ, "localhost");
        pmon = pq.find();
        Assert.assertNotNull(pmon);
        Assert.assertEquals("root1", pmon.getSshUsername());
        Assert.assertEquals("password1", pmon.getSshPassword());
        Assert.assertEquals(3322, pmon.getSshPort());
        Assert.assertEquals(5678, pmon.getMonPort());
    }
}
