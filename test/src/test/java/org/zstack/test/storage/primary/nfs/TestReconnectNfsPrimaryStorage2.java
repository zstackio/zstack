package org.zstack.test.storage.primary.nfs;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.host.HostInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.storage.primary.PrimaryStorageCapacityVO;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.simulator.storage.primary.nfs.NfsPrimaryStorageSimulatorConfig;
import org.zstack.storage.primary.nfs.NfsPrimaryStorageKVMBackendCommands.RemountCmd;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.storage.backup.sftp.TestSftpBackupStorageDeleteImage2;
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.logging.CLogger;

/**
 * 1. resize the nfs primary storage
 * 2. reconnect the host in the cluster attached to the nfs
 * <p>
 * confirm the remount command is sent
 * confirm the nfs capacity is extended
 */
public class TestReconnectNfsPrimaryStorage2 {
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
        config.totalCapacity = SizeUnit.TERABYTE.toByte(2);
        config.availableCapacity = SizeUnit.GIGABYTE.toByte(10);

        HostInventory host = deployer.hosts.get("host1");
        api.reconnectHost(host.getUuid());

        Assert.assertEquals(1, config.remountCmds.size());
        RemountCmd cmd = config.remountCmds.get(0);
        Assert.assertEquals(ps.getMountPath(), cmd.mountPath);
        Assert.assertEquals(ps.getUrl(), cmd.url);

        PrimaryStorageCapacityVO cap = dbf.findByUuid(ps.getUuid(), PrimaryStorageCapacityVO.class);
        Assert.assertEquals(config.totalCapacity, cap.getTotalCapacity());
        Assert.assertEquals(config.totalCapacity, cap.getTotalPhysicalCapacity());
        Assert.assertEquals(config.availableCapacity, cap.getAvailablePhysicalCapacity());
    }
}
