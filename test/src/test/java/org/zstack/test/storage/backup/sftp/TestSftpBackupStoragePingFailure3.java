package org.zstack.test.storage.backup.sftp;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.config.GlobalConfigFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.message.BeforeDeliveryMessageInterceptor;
import org.zstack.header.message.Message;
import org.zstack.header.storage.backup.BackupStorageStatus;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.storage.backup.ConnectBackupStorageMsg;
import org.zstack.simulator.storage.backup.sftp.SftpBackupStorageSimulatorConfig;
import org.zstack.storage.backup.BackupStorageGlobalConfig;
import org.zstack.storage.backup.sftp.SftpBackupStorageInventory;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.concurrent.TimeUnit;

public class TestSftpBackupStoragePingFailure3 {
    CLogger logger = Utils.getLogger(TestSftpBackupStoragePingFailure3.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    GlobalConfigFacade gcf;
    SessionInventory session;
    SftpBackupStorageSimulatorConfig config;
    boolean success;

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
        BackupStorageGlobalConfig.PING_INTERVAL.updateValue(1);
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        bus.installBeforeDeliveryMessageInterceptor(new BeforeDeliveryMessageInterceptor() {
            @Override
            public int orderOfBeforeDeliveryMessageInterceptor() {
                return 0;
            }

            @Override
            public void intercept(Message msg) {
                success = true;
            }
        }, ConnectBackupStorageMsg.class);

        SftpBackupStorageTestHelper helper = new SftpBackupStorageTestHelper();
        SftpBackupStorageInventory sinv = helper.addSimpleHttpBackupStorage(api);

        config.bsUuid = null;
        TimeUnit.SECONDS.sleep(3);
        Assert.assertTrue(success);
        BackupStorageVO bvo = dbf.findByUuid(sinv.getUuid(), BackupStorageVO.class);
        Assert.assertEquals(BackupStorageStatus.Connected, bvo.getStatus());
    }
}