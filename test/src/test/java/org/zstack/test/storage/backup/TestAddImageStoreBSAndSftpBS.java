package org.zstack.test.storage.backup;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.config.GlobalConfigFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.storage.backup.APIAddBackupStorageEvent;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.simulator.storage.backup.sftp.SftpBackupStorageSimulatorConfig;
import org.zstack.storage.backup.imagestore.APIAddImageStoreBackupStorageEvent;
import org.zstack.storage.backup.imagestore.APIAddImageStoreBackupStorageMsg;
import org.zstack.storage.backup.imagestore.ImageStoreBackupStorageConstant;
import org.zstack.storage.backup.imagestore.ImageStoreBackupStorageSimulatorConfig;
import org.zstack.storage.backup.sftp.*;
import org.zstack.test.*;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.deployer.schema.ImageStoreBackupStorageConfig;
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

/**
 * Created by miao on 16-7-27.
 */
public class TestAddImageStoreBSAndSftpBS {
    CLogger logger = Utils.getLogger(TestAddImageStoreBSAndSftpBS.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    GlobalConfigFacade gcf;
    SftpBackupStorageSimulatorConfig sftpConfig;
    ImageStoreBackupStorageSimulatorConfig isConfig;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/backupStorage/TestAddImageStoreBSAndSftpBS.xml", con);
        deployer.addSpringConfig("SftpBackupStorage.xml");
        deployer.addSpringConfig("SftpBackupStorageSimulator.xml");
        deployer.addSpringConfig("imagestore.xml");
        deployer.addSpringConfig("ImageStoreBackupStorageSimulator.xml");
        deployer.addSpringConfig("apimediator.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        gcf = loader.getComponent(GlobalConfigFacade.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        sftpConfig = loader.getComponent(SftpBackupStorageSimulatorConfig.class);
        isConfig = loader.getComponent(ImageStoreBackupStorageSimulatorConfig.class);
        session = api.loginAsAdmin();
    }

    @Test(expected = ApiSenderException.class)
    public void test() throws ApiSenderException {
        // add sftp backupStorage
        sftpConfig.connectSuccess = true;
        sftpConfig.totalCapacity = SizeUnit.GIGABYTE.toByte(100);
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
        BackupStorageInventory binv = evt.getInventory();
        SftpBackupStorageInventory inv = JSONObjectUtil.rehashObject(binv, SftpBackupStorageInventory.class);
        SftpBackupStorageVO vo = dbf.findByUuid(inv.getUuid(), SftpBackupStorageVO.class);
        Assert.assertEquals(vo.getHostname(), "localhost");


        // add imageStore backupStorage
        isConfig.connectSuccess = true;
        isConfig.totalCapacity = SizeUnit.GIGABYTE.toByte(100);
        APIAddImageStoreBackupStorageMsg msg1 = new APIAddImageStoreBackupStorageMsg();
        msg1.setSession(session);
        msg1.setName("TestImageStoreBackupStorage");
        msg1.setUrl("/is");
        msg1.setType(ImageStoreBackupStorageConstant.IMAGE_STORE_BACKUP_STORAGE_TYPE);
        msg1.setHostname("localhost");
        msg1.setUsername("root");
        msg1.setPassword("password");


        // add imageStore should trigger an exception
        boolean s = false;
        try {
            sender.send(msg1, APIAddImageStoreBackupStorageEvent.class);
        } catch (ApiSenderException e) {
            s = true;
        }
        Assert.assertTrue(s);
    }
}
