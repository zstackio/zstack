package org.zstack.test.kvm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.compute.host.HostGlobalConfig;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.host.HostInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.concurrent.TimeUnit;

/**
 * @author frank
 * @condition 1. change host check interval to 1s
 * 2. create a kvm host
 * 3. change host uuid on kvm simulator to mock a kvmagent restart
 * 4. wait for 3s
 * @test check host uuid on kvm simulator to confirm host reconnect happened
 */
public class TestKvmAgentRestart {
    CLogger logger = Utils.getLogger(TestKvmAgentRestart.class);
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
        deployer = new Deployer("deployerXml/kvm/TestKvmAgentRestart.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(KVMSimulatorConfig.class);
        HostGlobalConfig.PING_HOST_INTERVAL.updateValue(1);
        deployer.build();
        api = deployer.getApi();
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws InterruptedException {
        HostInventory host = deployer.hosts.get("host1");
        config.connectHostUuids.put(host.getUuid(), "wrong_host_uuid");
        TimeUnit.SECONDS.sleep(3);
        Assert.assertEquals(host.getUuid(), config.connectHostUuids.get(host.getUuid()));
    }

}
