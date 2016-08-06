package org.zstack.test.kvm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.compute.host.HostGlobalConfig;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostStatus;
import org.zstack.header.host.HostVO;
import org.zstack.header.identity.SessionInventory;
import org.zstack.simulator.storage.backup.sftp.SftpBackupStorageSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.concurrent.TimeUnit;

/**
 *
 * use KVMPingAgentExtensionForTest
 * set host ping interval to 1s
 *
 * 1. set KVMPingAgentExtensionForTest.success = true;
 *
 * confirm the host is connected
 *
 * 2. set KVMPingAgentExtensionForTest.success = false
 *
 * confirm the host is disconnected
 */
public class TestKvmPingAgentExtension {
    CLogger logger = Utils.getLogger(TestKvmPingAgentExtension.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    SftpBackupStorageSimulatorConfig config;
    KVMPingAgentExtensionForTest ext;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/kvm/TestCreateVmOnKvm.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("KVMPingAgentExtensionForTest.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(SftpBackupStorageSimulatorConfig.class);
        ext = loader.getComponent(KVMPingAgentExtensionForTest.class);
        session = api.loginAsAdmin();
    }
    
	@Test
	public void test() throws ApiSenderException, InterruptedException {
        HostInventory host = deployer.hosts.get("host1");

        HostGlobalConfig.PING_HOST_INTERVAL.updateValue(1);
        TimeUnit.SECONDS.sleep(3);

        HostVO vo = dbf.findByUuid(host.getUuid(), HostVO.class);
        Assert.assertEquals(HostStatus.Connected, vo.getStatus());

        ext.success = false;
        TimeUnit.SECONDS.sleep(3);

        vo = dbf.findByUuid(host.getUuid(), HostVO.class);
        Assert.assertEquals(HostStatus.Disconnected, vo.getStatus());
    }
}
