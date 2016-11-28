package org.zstack.test.storage.primary.nfs;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.storage.primary.PrimaryStorageStatus;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.simulator.storage.primary.nfs.NfsPrimaryStorageSimulatorConfig;
import org.zstack.storage.primary.nfs.NfsPrimaryStorageKVMBackendCommands.PingCmd;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.storage.backup.sftp.TestSftpBackupStorageDeleteImage2;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/**
 * 1. reconnect nfs primary storage
 * <p>
 * confirm the ping command sent
 * <p>
 * 2. make the ping command fail
 * 3. reconnect
 * <p>
 * confirm the primary storage disconnected
 * <p>
 * 3. make the ping command success
 * 4. reconnect
 * <p>
 * confirm the primary storage connected
 */
public class TestReconnectNfsPrimaryStorage4 {
    CLogger logger = Utils.getLogger(TestSftpBackupStorageDeleteImage2.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    NfsPrimaryStorageSimulatorConfig config;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/nfsPrimaryStorage/TestReconnectNfsPrimaryStorage.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(NfsPrimaryStorageSimulatorConfig.class);
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException {
        PrimaryStorageInventory ps = deployer.primaryStorages.get("nfs");
        config.pingCmds.clear();
        api.reconnectPrimaryStorage(ps.getUuid());
        Assert.assertEquals(1, config.pingCmds.size());
        PingCmd cmd = config.pingCmds.get(0);
        Assert.assertEquals(ps.getUuid(), cmd.getUuid());

        config.pingSuccess = false;
        boolean s = false;
        try {
            api.reconnectPrimaryStorage(ps.getUuid());
        } catch (ApiSenderException e) {
            s = true;
        }
        Assert.assertTrue(s);
        PrimaryStorageVO psvo = dbf.findByUuid(ps.getUuid(), PrimaryStorageVO.class);
        Assert.assertEquals(PrimaryStorageStatus.Disconnected, psvo.getStatus());

        config.pingSuccess = true;
        api.reconnectPrimaryStorage(ps.getUuid());
        psvo = dbf.findByUuid(ps.getUuid(), PrimaryStorageVO.class);
        Assert.assertEquals(PrimaryStorageStatus.Connected, psvo.getStatus());
    }
}
