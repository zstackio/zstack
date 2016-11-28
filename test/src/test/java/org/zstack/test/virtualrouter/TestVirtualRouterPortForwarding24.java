package org.zstack.test.virtualrouter;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.network.service.portforwarding.PortForwardingRuleInventory;
import org.zstack.network.service.portforwarding.PortForwardingRuleVO;
import org.zstack.network.service.vip.VipInventory;
import org.zstack.network.service.vip.VipVO;
import org.zstack.network.service.virtualrouter.portforwarding.PortForwardingRuleTO;
import org.zstack.simulator.appliancevm.ApplianceVmSimulatorConfig;
import org.zstack.simulator.virtualrouter.VirtualRouterSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;

/**
 * @author frank
 * @condition 1. create a vm with port forwarding rule
 * 2. detach the rule
 * 3. delete rule
 * @test confirm port forwarding rule deleted
 */
public class TestVirtualRouterPortForwarding24 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    VirtualRouterSimulatorConfig vconfig;
    ApplianceVmSimulatorConfig aconfig;

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
        aconfig = loader.getComponent(ApplianceVmSimulatorConfig.class);
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException {
        PortForwardingRuleInventory pfRule1 = deployer.portForwardingRules.get("pfRule1");
        VipVO vipvo = dbf.findByUuid(pfRule1.getVipUuid(), VipVO.class);
        VipInventory vip = VipInventory.valueOf(vipvo);
        api.detachPortForwardingRule(pfRule1.getUuid());
        PortForwardingRuleTO removedRule = vconfig.removedPortForwardingRules.get(0);
        PortForwardingRuleTestValidator validator = new PortForwardingRuleTestValidator();
        Assert.assertTrue(validator.compare(removedRule, pfRule1));
        VipTestValidator.validateWithoutCheckOwnerEthernetMac(vconfig.removedVips, vip);

        validator.noFirewall(aconfig, pfRule1);

        api.revokePortForwardingRule(pfRule1.getUuid());
        PortForwardingRuleVO rulevo = dbf.findByUuid(pfRule1.getUuid(), PortForwardingRuleVO.class);
        Assert.assertNull(rulevo);
        vipvo = dbf.findByUuid(pfRule1.getVipUuid(), VipVO.class);
        Assert.assertNotNull(vipvo);
        Assert.assertNull(vipvo.getUseFor());
    }
}
