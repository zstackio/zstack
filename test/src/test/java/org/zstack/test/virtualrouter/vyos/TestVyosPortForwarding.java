package org.zstack.test.virtualrouter.vyos;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.network.service.portforwarding.PortForwardingProtocolType;
import org.zstack.network.service.portforwarding.PortForwardingRuleInventory;
import org.zstack.network.service.vip.VipInventory;
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO;
import org.zstack.network.service.virtualrouter.portforwarding.PortForwardingRuleTO;
import org.zstack.simulator.appliancevm.ApplianceVmSimulatorConfig;
import org.zstack.simulator.virtualrouter.VirtualRouterSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.virtualrouter.PortForwardingRuleTestValidator;
import org.zstack.test.virtualrouter.VipTestValidator;

/**
 * 
 * @author frank
 * 
 * @condition
 * use vyos provider
 *
 * 1. create a vm
 * 2. acquire a public ip: pub1
 * 3. set port forwarding to vm using pub1
 *
 * @test
 * confirm port forwarding rules on vm are correct
 */
public class TestVyosPortForwarding {
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
        deployer = new Deployer("deployerXml/virtualRouter/TestVyosPortForwarding.xml", con);
        deployer.addSpringConfig("VirtualRouter.xml");
        deployer.addSpringConfig("VirtualRouterSimulator.xml");
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("PortForwarding.xml");
        deployer.addSpringConfig("vip.xml");
        deployer.addSpringConfig("vyos.xml");
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
        L3NetworkInventory publicNw = deployer.l3Networks.get("PublicNetwork");
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        VipInventory vip = api.acquireIp(publicNw.getUuid());
        PortForwardingRuleInventory rule = new PortForwardingRuleInventory();
        rule.setName("test name");
        rule.setAllowedCidr("72.1.1.1/24");
        rule.setPrivatePortEnd(100);
        rule.setPrivatePortStart(80);
        rule.setProtocolType(PortForwardingProtocolType.TCP.toString());
        rule.setVipUuid(vip.getUuid());
        rule.setVipPortEnd(100);
        rule.setVipPortStart(80);
        rule.setVmNicUuid(vm.getVmNics().get(0).getUuid());
        rule = api.createPortForwardingRuleByFullConfig(rule);

        VipTestValidator vipValidator = new VipTestValidator();
        vipValidator.validate(vconfig.vips, vip);
        PortForwardingRuleTO to = vconfig.portForwardingRules.get(0);
        PortForwardingRuleTestValidator validator = new PortForwardingRuleTestValidator();
        Assert.assertTrue(validator.compare(to, rule));

        // test rules are synced after the vyos vr reboots
        vconfig.portForwardingRules.clear();
        VirtualRouterVmVO vr = dbf.listAll(VirtualRouterVmVO.class).get(0);
        api.rebootVmInstance(vr.getUuid());
        Assert.assertFalse(vconfig.portForwardingRules.isEmpty());
    }
}
