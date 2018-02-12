package org.zstack.test.console;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.console.ConsoleGlobalConfig;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.console.ReconnectConsoleProxyMsg;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.message.AbstractBeforeDeliveryMessageInterceptor;
import org.zstack.header.message.Message;
import org.zstack.simulator.consoleproxy.ConsoleProxySimulatorConfig;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 10:00 PM
 * To change this template use File | Settings | File Templates.
 */
public class TestConsoleProxy6 {
    CLogger logger = Utils.getLogger(TestConsoleProxy6.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    KVMSimulatorConfig config;
    ConsoleProxySimulatorConfig consoleConfig;
    boolean success;

    @Before
    public void setUp() throws Exception {
        System.setProperty("management.server.ip", "127.0.0.1");

        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/kvm/TestCreateVmOnKvm.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("ConsoleManager.xml");
        deployer.addSpringConfig("ConsoleSimulator.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        consoleConfig = loader.getComponent(ConsoleProxySimulatorConfig.class);
        config = loader.getComponent(KVMSimulatorConfig.class);
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        bus.installBeforeDeliveryMessageInterceptor(new AbstractBeforeDeliveryMessageInterceptor() {
            @Override
            public void intercept(Message msg) {
                success = true;
                throw new CloudRuntimeException("ok, don't delivery, because we don't really have an agent to redeloy");
            }
        }, ReconnectConsoleProxyMsg.class);

        consoleConfig.pingSuccess = false;
        ConsoleGlobalConfig.PING_INTERVAL.updateValue(1);
        TimeUnit.SECONDS.sleep(3);
        consoleConfig.pingSuccess = true;
        TimeUnit.SECONDS.sleep(3);

        Assert.assertTrue(success);
        Assert.assertFalse(consoleConfig.pingCmdList.isEmpty());
    }
}
