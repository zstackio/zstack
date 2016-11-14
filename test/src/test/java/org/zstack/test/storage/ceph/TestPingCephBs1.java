package org.zstack.test.storage.ceph;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.storage.backup.BackupStorageStatus;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.storage.backup.BackupStorageGlobalConfig;
import org.zstack.storage.ceph.CephGlobalConfig;
import org.zstack.storage.ceph.MonStatus;
import org.zstack.storage.ceph.backup.CephBackupStorageBase.PingOperationFailure;
import org.zstack.storage.ceph.backup.CephBackupStorageMonVO;
import org.zstack.storage.ceph.backup.CephBackupStorageSimulatorConfig;
import org.zstack.storage.ceph.backup.CephBackupStorageVO;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;

/**
 * test ping ceph backup storage mons
 */
public class TestPingCephBs1 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    KVMSimulatorConfig kconfig;
    CephBackupStorageSimulatorConfig config;

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
        config = loader.getComponent(CephBackupStorageSimulatorConfig.class);
        kconfig = loader.getComponent(KVMSimulatorConfig.class);
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        BackupStorageInventory bs = deployer.backupStorages.get("ceph-bk");
        CephBackupStorageVO ceph = dbf.findByUuid(bs.getUuid(), CephBackupStorageVO.class);
        Iterator<CephBackupStorageMonVO> it = ceph.getMons().iterator();
        CephBackupStorageMonVO mon1 = it.next();
        CephBackupStorageMonVO mon2 = it.next();

        BackupStorageGlobalConfig.PING_INTERVAL.updateValue(1);
        CephGlobalConfig.BACKUP_STORAGE_MON_AUTO_RECONNECT.updateValue(false);
        CephGlobalConfig.BACKUP_STORAGE_MON_RECONNECT_DELAY.updateValue(0);
        // put one mon down, the backup storage is still up because another mon is up
        config.pingCmdSuccess.put(mon1.getUuid(), false);
        TimeUnit.SECONDS.sleep(3);
        ceph = dbf.reload(ceph);
        mon1 = dbf.reload(mon1);
        mon2 = dbf.reload(mon2);
        Assert.assertEquals(BackupStorageStatus.Connected, ceph.getStatus());
        Assert.assertEquals(MonStatus.Disconnected, mon1.getStatus());
        Assert.assertEquals(MonStatus.Connected, mon2.getStatus());

        // put all mon down, the backup storage is down
        config.pingCmdSuccess.put(mon2.getUuid(), false);
        TimeUnit.SECONDS.sleep(3);
        ceph = dbf.reload(ceph);
        mon1 = dbf.reload(mon1);
        mon2 = dbf.reload(mon2);
        Assert.assertEquals(BackupStorageStatus.Disconnected, ceph.getStatus());
        Assert.assertEquals(MonStatus.Disconnected, mon1.getStatus());
        Assert.assertEquals(MonStatus.Disconnected, mon2.getStatus());

        // put one mon up, the backup storage is up
        config.pingCmdSuccess.put(mon1.getUuid(), true);
        TimeUnit.SECONDS.sleep(3);
        ceph = dbf.reload(ceph);
        mon1 = dbf.reload(mon1);
        mon2 = dbf.reload(mon2);
        Assert.assertEquals(BackupStorageStatus.Connected, ceph.getStatus());
        Assert.assertEquals(MonStatus.Connected, mon1.getStatus());
        Assert.assertEquals(MonStatus.Disconnected, mon2.getStatus());

        // put all mons up
        config.pingCmdSuccess.put(mon2.getUuid(), true);
        CephGlobalConfig.BACKUP_STORAGE_MON_AUTO_RECONNECT.updateValue(true);
        TimeUnit.SECONDS.sleep(3);
        ceph = dbf.reload(ceph);
        mon1 = dbf.reload(mon1);
        mon2 = dbf.reload(mon2);
        Assert.assertEquals(BackupStorageStatus.Connected, ceph.getStatus());
        Assert.assertEquals(MonStatus.Connected, mon1.getStatus());
        Assert.assertEquals(MonStatus.Connected, mon2.getStatus());
        CephGlobalConfig.BACKUP_STORAGE_MON_AUTO_RECONNECT.updateValue(false);

        // put all mons to operation failure, confirm all down
        config.pingCmdSuccess.put(mon1.getUuid(), false);
        config.pingCmdSuccess.put(mon2.getUuid(), false);
        config.pingCmdOperationFailure.put(mon1.getUuid(), PingOperationFailure.UnableToCreateFile);
        TimeUnit.SECONDS.sleep(3);
        ceph = dbf.reload(ceph);
        mon1 = dbf.reload(mon1);
        mon2 = dbf.reload(mon2);
        Assert.assertEquals(BackupStorageStatus.Disconnected, ceph.getStatus());
        Assert.assertEquals(MonStatus.Disconnected, mon1.getStatus());
        Assert.assertEquals(MonStatus.Disconnected, mon2.getStatus());

        // put one mon to the error MonAddrChanged, confirm only the mon down
        config.pingCmdSuccess.put(mon1.getUuid(), false);
        config.pingCmdSuccess.put(mon2.getUuid(), true);
        config.pingCmdOperationFailure.put(mon1.getUuid(), PingOperationFailure.MonAddrChanged);
        TimeUnit.SECONDS.sleep(3);
        ceph = dbf.reload(ceph);
        mon1 = dbf.reload(mon1);
        mon2 = dbf.reload(mon2);
        Assert.assertEquals(BackupStorageStatus.Connected, ceph.getStatus());
        Assert.assertEquals(MonStatus.Disconnected, mon1.getStatus());
        Assert.assertEquals(MonStatus.Connected, mon2.getStatus());

        // put all mons up, the backup storage is up
        CephGlobalConfig.BACKUP_STORAGE_MON_AUTO_RECONNECT.updateValue(true);
        config.pingCmdSuccess.clear();
        config.pingCmdOperationFailure.clear();
        TimeUnit.SECONDS.sleep(3);
        ceph = dbf.reload(ceph);
        mon1 = dbf.reload(mon1);
        mon2 = dbf.reload(mon2);
        Assert.assertEquals(BackupStorageStatus.Connected, ceph.getStatus());
        Assert.assertEquals(MonStatus.Connected, mon1.getStatus());
        Assert.assertEquals(MonStatus.Connected, mon2.getStatus());
    }
}
