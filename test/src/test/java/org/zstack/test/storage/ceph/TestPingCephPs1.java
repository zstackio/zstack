package org.zstack.test.storage.ceph;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.storage.primary.PrimaryStorageStatus;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.storage.ceph.CephGlobalConfig;
import org.zstack.storage.ceph.MonStatus;
import org.zstack.storage.ceph.backup.CephBackupStorageSimulatorConfig;
import org.zstack.storage.ceph.primary.CephPrimaryStorageMonBase.PingOperationFailure;
import org.zstack.storage.ceph.primary.CephPrimaryStorageMonVO;
import org.zstack.storage.ceph.primary.CephPrimaryStorageSimulatorConfig;
import org.zstack.storage.ceph.primary.CephPrimaryStorageVO;
import org.zstack.storage.primary.PrimaryStorageGlobalConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;

/**
 * test ping ceph primary storage mons
 */
public class TestPingCephPs1 {
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
        deployer = new Deployer("deployerXml/ceph/TestPingCephPs1.xml", con);
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
    public void test() throws ApiSenderException, InterruptedException {
        PrimaryStorageInventory ps = deployer.primaryStorages.get("ceph-pri");
        CephPrimaryStorageVO ceph = dbf.findByUuid(ps.getUuid(), CephPrimaryStorageVO.class);
        Iterator<CephPrimaryStorageMonVO> it = ceph.getMons().iterator();
        CephPrimaryStorageMonVO mon1 = it.next();
        CephPrimaryStorageMonVO mon2 = it.next();

        PrimaryStorageGlobalConfig.PING_INTERVAL.updateValue(1);
        CephGlobalConfig.PRIMARY_STORAGE_MON_AUTO_RECONNECT.updateValue(false);
        CephGlobalConfig.PRIMARY_STORAGE_MON_RECONNECT_DELAY.updateValue(0);
        // put one mon down, the primary storage is still up because another mon is up
        config.pingCmdSuccess.put(mon1.getUuid(), false);
        TimeUnit.SECONDS.sleep(3);
        ceph = dbf.reload(ceph);
        mon1 = dbf.reload(mon1);
        mon2 = dbf.reload(mon2);
        Assert.assertEquals(PrimaryStorageStatus.Connected, ceph.getStatus());
        Assert.assertEquals(MonStatus.Disconnected, mon1.getStatus());
        Assert.assertEquals(MonStatus.Connected, mon2.getStatus());

        // put all mon down, the primary storage is down
        config.pingCmdSuccess.put(mon2.getUuid(), false);
        TimeUnit.SECONDS.sleep(3);
        ceph = dbf.reload(ceph);
        mon1 = dbf.reload(mon1);
        mon2 = dbf.reload(mon2);
        Assert.assertEquals(PrimaryStorageStatus.Disconnected, ceph.getStatus());
        Assert.assertEquals(MonStatus.Disconnected, mon1.getStatus());
        Assert.assertEquals(MonStatus.Disconnected, mon2.getStatus());

        // put one mon up, the primary storage is up
        config.pingCmdSuccess.put(mon1.getUuid(), true);
        TimeUnit.SECONDS.sleep(3);
        ceph = dbf.reload(ceph);
        mon1 = dbf.reload(mon1);
        mon2 = dbf.reload(mon2);
        Assert.assertEquals(PrimaryStorageStatus.Connected, ceph.getStatus());
        Assert.assertEquals(MonStatus.Connected, mon1.getStatus());
        Assert.assertEquals(MonStatus.Disconnected, mon2.getStatus());

        // put all mons up
        config.pingCmdSuccess.put(mon2.getUuid(), true);
        CephGlobalConfig.PRIMARY_STORAGE_MON_AUTO_RECONNECT.updateValue(true);
        TimeUnit.SECONDS.sleep(3);
        ceph = dbf.reload(ceph);
        mon1 = dbf.reload(mon1);
        mon2 = dbf.reload(mon2);
        Assert.assertEquals(PrimaryStorageStatus.Connected, ceph.getStatus());
        Assert.assertEquals(MonStatus.Connected, mon1.getStatus());
        Assert.assertEquals(MonStatus.Connected, mon2.getStatus());
        CephGlobalConfig.PRIMARY_STORAGE_MON_AUTO_RECONNECT.updateValue(false);

        // put all mons to operation failure, confirm all down
        config.pingCmdSuccess.put(mon1.getUuid(), false);
        config.pingCmdSuccess.put(mon2.getUuid(), false);
        config.pingCmdOperationFailure.put(mon1.getUuid(), PingOperationFailure.UnableToCreateFile);
        TimeUnit.SECONDS.sleep(3);
        ceph = dbf.reload(ceph);
        mon1 = dbf.reload(mon1);
        mon2 = dbf.reload(mon2);
        Assert.assertEquals(PrimaryStorageStatus.Disconnected, ceph.getStatus());
        Assert.assertEquals(MonStatus.Disconnected, mon1.getStatus());
        Assert.assertEquals(MonStatus.Disconnected, mon2.getStatus());

        // put one mon in monAddrChanged error, confirm only that mon down
        config.pingCmdSuccess.put(mon1.getUuid(), false);
        config.pingCmdSuccess.put(mon2.getUuid(), true);
        config.pingCmdOperationFailure.put(mon1.getUuid(), PingOperationFailure.MonAddrChanged);
        TimeUnit.SECONDS.sleep(3);
        ceph = dbf.reload(ceph);
        mon1 = dbf.reload(mon1);
        mon2 = dbf.reload(mon2);
        Assert.assertEquals(PrimaryStorageStatus.Connected, ceph.getStatus());
        Assert.assertEquals(MonStatus.Disconnected, mon1.getStatus());
        Assert.assertEquals(MonStatus.Connected, mon2.getStatus());

        // put all mons up, the primary storage is up
        CephGlobalConfig.PRIMARY_STORAGE_MON_AUTO_RECONNECT.updateValue(true);
        config.pingCmdSuccess.clear();
        config.pingCmdOperationFailure.clear();
        TimeUnit.SECONDS.sleep(3);
        ceph = dbf.reload(ceph);
        mon1 = dbf.reload(mon1);
        mon2 = dbf.reload(mon2);
        Assert.assertEquals(PrimaryStorageStatus.Connected, ceph.getStatus());
        Assert.assertEquals(MonStatus.Connected, mon1.getStatus());
        Assert.assertEquals(MonStatus.Connected, mon2.getStatus());
    }
}
