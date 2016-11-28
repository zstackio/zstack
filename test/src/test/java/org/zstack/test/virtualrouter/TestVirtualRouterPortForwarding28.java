package org.zstack.test.virtualrouter;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.network.service.portforwarding.PortForwardingRuleInventory;
import org.zstack.network.service.portforwarding.PortForwardingRuleState;
import org.zstack.network.service.portforwarding.PortForwardingRuleStateEvent;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.simulator.virtualrouter.VirtualRouterSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;

/**
 * @author frank
 * @condition 1. change port forwarding rule state
 * @test confirm state change success
 */
public class TestVirtualRouterPortForwarding28 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    VirtualRouterSimulatorConfig vconfig;
    KVMSimulatorConfig kconfig;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/virtualRouter/TestVirtualRouterPortForwarding2.xml", con);
        deployer.addSpringConfig("VirtualRouter.xml");
        deployer.addSpringConfig("VirtualRouterSimulator.xml");
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("PortForwarding.xml");
        deployer.addSpringConfig("vip.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        vconfig = loader.getComponent(VirtualRouterSimulatorConfig.class);
        kconfig = loader.getComponent(KVMSimulatorConfig.class);
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException {
        PortForwardingRuleInventory pfRule1 = deployer.portForwardingRules.get("pfRule1");
        Assert.assertEquals(PortForwardingRuleState.Enabled.toString(), pfRule1.getState());
        pfRule1 = api.changePortForwardingRuleState(pfRule1.getUuid(), PortForwardingRuleStateEvent.disable);
        Assert.assertEquals(PortForwardingRuleState.Disabled.toString(), pfRule1.getState());
        pfRule1 = api.changePortForwardingRuleState(pfRule1.getUuid(), PortForwardingRuleStateEvent.enable);
        Assert.assertEquals(PortForwardingRuleState.Enabled.toString(), pfRule1.getState());
    }
}
