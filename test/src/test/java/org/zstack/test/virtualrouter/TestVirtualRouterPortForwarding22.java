package org.zstack.test.virtualrouter;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.service.portforwarding.PortForwardingProtocolType;
import org.zstack.network.service.portforwarding.PortForwardingRuleInventory;
import org.zstack.network.service.vip.VipInventory;
import org.zstack.network.service.vip.VipVO;
import org.zstack.network.service.virtualrouter.portforwarding.VirtualRouterPortForwardingRuleRefVO;
import org.zstack.network.service.virtualrouter.vip.VirtualRouterVipVO;
import org.zstack.simulator.appliancevm.ApplianceVmSimulatorConfig;
import org.zstack.simulator.virtualrouter.VirtualRouterSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;

import java.util.ArrayList;
import java.util.List;

/**
 * @author frank
 * @condition 1. create a vm
 * 2. acquire a vip
 * 3. use the vip for two port forwarding rules
 * 4. detach all rules
 * @test confirm all rules and vip are removed
 */
public class TestVirtualRouterPortForwarding22 {
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
        deployer = new Deployer("deployerXml/virtualRouter/TestVirtualRouterPortForwarding9.xml", con);
        deployer.addSpringConfig("VirtualRouter.xml");
        deployer.addSpringConfig("VirtualRouterSimulator.xml");
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("vip.xml");
        deployer.addSpringConfig("PortForwarding.xml");
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
        PortForwardingRuleInventory rule1 = new PortForwardingRuleInventory();
        PortForwardingRuleInventory rule2 = new PortForwardingRuleInventory();
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        VmNicInventory nic = vm.getVmNics().get(0);
        L3NetworkInventory vipNw = deployer.l3Networks.get("PublicNetwork");
        VipInventory vip = api.acquireIp(vipNw.getUuid());

        rule1.setName("pfRule1");
        rule1.setVipUuid(vip.getUuid());
        rule1.setVmNicUuid(nic.getUuid());
        rule1.setVipPortStart(22);
        rule1.setVipPortEnd(100);
        rule1.setPrivatePortStart(22);
        rule1.setPrivatePortEnd(100);
        rule1.setProtocolType(PortForwardingProtocolType.TCP.toString());
        rule1 = api.createPortForwardingRuleByFullConfig(rule1);

        rule2.setName("pfRule2");
        rule2.setVipUuid(vip.getUuid());
        rule2.setVmNicUuid(nic.getUuid());
        rule2.setVipPortStart(1000);
        rule2.setVipPortEnd(2000);
        rule2.setPrivatePortStart(1000);
        rule2.setPrivatePortEnd(2000);
        rule2.setProtocolType(PortForwardingProtocolType.TCP.toString());
        rule2 = api.createPortForwardingRuleByFullConfig(rule2);

        PortForwardingRuleTestValidator validator = new PortForwardingRuleTestValidator();
        validator.validate(vconfig.portForwardingRules, deployer.portForwardingRules.values());

        api.detachPortForwardingRule(rule1.getUuid());
        api.detachPortForwardingRule(rule2.getUuid());
        Assert.assertEquals(2, vconfig.removedPortForwardingRules.size());
        List<PortForwardingRuleInventory> rules = new ArrayList<PortForwardingRuleInventory>();
        rules.add(rule1);
        rules.add(rule2);
        validator.validate(vconfig.removedPortForwardingRules, rules);
        Assert.assertFalse(dbf.isExist(vip.getUuid(), VirtualRouterVipVO.class));
        Assert.assertFalse(dbf.isExist(rule2.getUuid(), VirtualRouterPortForwardingRuleRefVO.class));
        VipVO vipvo = dbf.findByUuid(vip.getUuid(), VipVO.class);
        // the rules are just detached, not deleted
        // so vip still keeps below information
        Assert.assertNotNull(vipvo.getPeerL3NetworkUuid());
        Assert.assertNotNull(vipvo.getServiceProvider());

        validator.noFirewall(aconfig, rule1);
        validator.noFirewall(aconfig, rule2);
    }
}
