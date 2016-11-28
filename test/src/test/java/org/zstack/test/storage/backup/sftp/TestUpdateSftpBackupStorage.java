package org.zstack.test.storage.backup.sftp;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.config.GlobalConfigFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.storage.backup.APIAddBackupStorageEvent;
import org.zstack.simulator.storage.backup.sftp.SftpBackupStorageSimulatorConfig;
import org.zstack.storage.backup.sftp.*;
import org.zstack.test.*;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.logging.CLogger;

public class TestUpdateSftpBackupStorage {
    CLogger logger = Utils.getLogger(TestUpdateSftpBackupStorage.class);
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
        config = loader.getComponent(SftpBackupStorageSimulatorConfig.class);
        gcf = loader.getComponent(GlobalConfigFacade.class);
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException {
        config.connectSuccess = true;
        config.totalCapacity = SizeUnit.GIGABYTE.toByte(100);
        APIAddSftpBackupStorageMsg msg = new APIAddSftpBackupStorageMsg();
        msg.setSession(session);
        msg.setName("TestBackupStorage");
        msg.setUrl("/backupstorage");
        msg.setType(SftpBackupStorageConstant.SFTP_BACKUP_STORAGE_TYPE);
        msg.setHostname("localhost");
        msg.setUsername("root");
        msg.setPassword("password");
        ApiSender sender = api.getApiSender();
        APIAddBackupStorageEvent evt = sender.send(msg, APIAddSftpBackupStorageEvent.class);
        SftpBackupStorageInventory binv = (SftpBackupStorageInventory) evt.getInventory();
        binv.setName("1");
        binv.setDescription("xxx");
        binv.setUsername("admin");
        binv = (SftpBackupStorageInventory) api.updateSftpBackupStorage(binv, "admin");
        Assert.assertEquals("1", binv.getName());
        Assert.assertEquals("xxx", binv.getDescription());
        Assert.assertEquals("admin", binv.getUsername());

        SftpBackupStorageVO vo = dbf.findByUuid(binv.getUuid(), SftpBackupStorageVO.class);
        Assert.assertEquals("admin", vo.getPassword());
    }
}
