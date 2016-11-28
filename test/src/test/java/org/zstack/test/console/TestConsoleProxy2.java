package org.zstack.test.console;

import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.simulator.consoleproxy.ConsoleProxySimulatorConfig;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 10:00 PM
 * To change this template use File | Settings | File Templates.
 * <p>
 * 1. start a vm
 * 2. request console
 * 3. set checking console availability success to false in simulator
 * 4. request console again
 * 5. ApiSenderException should be thrown out
 */
public class TestConsoleProxy2 {
    CLogger logger = Utils.getLogger(TestConsoleProxy2.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    KVMSimulatorConfig config;
    ConsoleProxySimulatorConfig consoleConfig;

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

    @Test(expected = ApiSenderException.class)
    public void test() throws ApiSenderException {
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        config.consolePort = 5901;
        consoleConfig.availableSuccess = false;
        api.getConsole(vm.getUuid());
        // the second time will get console information from database and check availability
        api.getConsole(vm.getUuid());
    }
}
