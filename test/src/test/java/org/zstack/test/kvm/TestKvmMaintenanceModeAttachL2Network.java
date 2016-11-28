package org.zstack.test.kvm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostStateEvent;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.storage.backup.sftp.TestSftpBackupStorageDeleteImage2;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.concurrent.TimeUnit;

/**
 * 1. have two hosts in cluster
 * 2. put host1 into maintenance mode
 * 3. attach l2 network to cluster
 * 4. enable host1
 * <p>
 * confirm after host gets out of maintenance mode, l2 network mounts on it
 */
public class TestKvmMaintenanceModeAttachL2Network {
    CLogger logger = Utils.getLogger(TestSftpBackupStorageDeleteImage2.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    KVMSimulatorConfig config;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/kvm/TestKvmMaintenanceClusterAttachResource.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(KVMSimulatorConfig.class);
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        HostInventory host1 = deployer.hosts.get("host1");
        api.maintainHost(host1.getUuid());
        L2NetworkInventory l2 = deployer.l2Networks.get("TestL2Network1");
        api.attachL2NetworkToCluster(l2.getUuid(), host1.getClusterUuid());
        Assert.assertEquals(1, config.createBridgeCmds.size());
        config.createBridgeCmds.clear();
        api.changeHostState(host1.getUuid(), HostStateEvent.enable);
        // wait for host reconnect
        TimeUnit.SECONDS.sleep(5);
        Assert.assertEquals(1, config.createBridgeCmds.size());
    }
}
