package org.zstack.test.storage.backup.sftp;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.config.GlobalConfigFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.simulator.storage.backup.sftp.SftpBackupStorageSimulatorConfig;
import org.zstack.storage.backup.sftp.APIAddSftpBackupStorageEvent;
import org.zstack.storage.backup.sftp.APIAddSftpBackupStorageMsg;
import org.zstack.storage.backup.sftp.SftpBackupStorageConstant;
import org.zstack.storage.backup.sftp.SftpBackupStorageVO;
import org.zstack.test.*;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.logging.CLogger;

public class TestAddSftpBackupStorageFailure {
    CLogger logger = Utils.getLogger(TestAddSftpBackupStorageFailure.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    GlobalConfigFacade gcf;
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
        gcf = loader.getComponent(GlobalConfigFacade.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(SftpBackupStorageSimulatorConfig.class);
        session = api.loginAsAdmin();
    }

    @Test(expected = ApiSenderException.class)
    public void test() throws ApiSenderException {
        config.connectSuccess = false;
        config.totalCapacity = SizeUnit.GIGABYTE.toByte(100);
        config.usedCapacity = SizeUnit.GIGABYTE.toByte(10);
        APIAddSftpBackupStorageMsg msg = new APIAddSftpBackupStorageMsg();
        msg.setSession(session);
        msg.setName("TestBackupStorage");
        msg.setUrl("/backupstorage");
        msg.setType(SftpBackupStorageConstant.SFTP_BACKUP_STORAGE_TYPE);
        msg.setHostname("localhost");
        msg.setUsername("root");
        msg.setPassword("password");
        ApiSender sender = api.getApiSender();
        try {
            sender.send(msg, APIAddSftpBackupStorageEvent.class);
        } catch (ApiSenderException e) {
            long count = dbf.count(SftpBackupStorageVO.class);
            Assert.assertEquals(0, count);
            throw e;
        }
    }
}
