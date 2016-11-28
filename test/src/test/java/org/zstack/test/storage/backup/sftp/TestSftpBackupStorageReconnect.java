package org.zstack.test.storage.backup.sftp;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.config.GlobalConfigFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.storage.backup.BackupStorageStatus;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.simulator.storage.backup.sftp.SftpBackupStorageSimulatorConfig;
import org.zstack.storage.backup.sftp.SftpBackupStorageInventory;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class TestSftpBackupStorageReconnect {
    CLogger logger = Utils.getLogger(TestSftpBackupStorageReconnect.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    GlobalConfigFacade gcf;
    SessionInventory session;
    SftpBackupStorageSimulatorConfig config;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/sftpBackupStorage/TestAddSftpBackupStorage.xml", con);
        deployer.addSpringConfig("SftpBackupStorage.xml");
        deployer.addSpringConfig("SftpBackupStorageSimulator.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        gcf = loader.getComponent(GlobalConfigFacade.class);
        config = loader.getComponent(SftpBackupStorageSimulatorConfig.class);
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        SftpBackupStorageTestHelper helper = new SftpBackupStorageTestHelper();
        SftpBackupStorageInventory sinv = helper.addSimpleHttpBackupStorage(api);
        BackupStorageVO bs = dbf.findByUuid(sinv.getUuid(), BackupStorageVO.class);
        bs.setStatus(BackupStorageStatus.Disconnected);
        dbf.update(bs);

        sinv = api.reconnectSftpBackupStorage(sinv.getUuid());
        Assert.assertEquals(BackupStorageStatus.Connected.toString(), sinv.getStatus());
        bs = dbf.findByUuid(sinv.getUuid(), BackupStorageVO.class);
        Assert.assertEquals(BackupStorageStatus.Connected, bs.getStatus());

        bs.setStatus(BackupStorageStatus.Disconnected);
        dbf.update(bs);
        api.reconnectBackupStorage(bs.getUuid());
        bs = dbf.findByUuid(sinv.getUuid(), BackupStorageVO.class);
        Assert.assertEquals(BackupStorageStatus.Connected, bs.getStatus());

        bs.setStatus(BackupStorageStatus.Disconnected);
        dbf.update(bs);
        BackupStorageInventory bsinv = api.reconnectBackupStorage(bs.getUuid());
        bs = dbf.findByUuid(sinv.getUuid(), BackupStorageVO.class);
        Assert.assertEquals(BackupStorageStatus.Connected.toString(), bsinv.getStatus());
    }
}