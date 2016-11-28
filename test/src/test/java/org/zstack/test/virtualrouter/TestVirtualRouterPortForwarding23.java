package org.zstack.test.virtualrouter;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.service.portforwarding.PortForwardingRuleInventory;
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
 * @condition 1. create vm1 with port forwarding rule
 * 2. detach the rule
 * 3. create vm2
 * 4. attach rule to vm2
 * @test confirm port forwarding rule detach/attach work
 */
public class TestVirtualRouterPortForwarding23 {
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

    private void checkRuleOff(final PortForwardingRuleInventory rule, VipInventory vip) {
        PortForwardingRuleTO removedRule = vconfig.removedPortForwardingRules.get(0);
        PortForwardingRuleTestValidator validator = new PortForwardingRuleTestValidator();
        Assert.assertTrue(validator.compare(removedRule, rule));
        VipTestValidator.validateWithoutCheckOwnerEthernetMac(vconfig.removedVips, vip);

        validator.noFirewall(aconfig, rule);
    }

    private void checkRuleOn(final PortForwardingRuleInventory rule, VipInventory vip) {
        VipTestValidator vipValidator = new VipTestValidator();
        vipValidator.validate(vconfig.vips, vip);
        PortForwardingRuleTO to = vconfig.portForwardingRules.get(0);
        PortForwardingRuleTestValidator validator = new PortForwardingRuleTestValidator();
        Assert.assertTrue(validator.compare(to, rule));

        validator.hasFirewall(aconfig, rule);
    }

    @Test
    public void test() throws ApiSenderException {
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        api.stopVmInstance(vm.getUuid());

        PortForwardingRuleInventory pfRule1 = deployer.portForwardingRules.get("pfRule1");
        VipVO vipvo = dbf.findByUuid(pfRule1.getVipUuid(), VipVO.class);
        VipInventory vip = VipInventory.valueOf(vipvo);
        api.detachPortForwardingRule(pfRule1.getUuid());
        checkRuleOff(pfRule1, vip);

        vconfig.portForwardingRules.clear();
        vconfig.vips.clear();
        vm = api.createVmFromClone(vm);
        Assert.assertEquals(0, vconfig.portForwardingRules.size());
        Assert.assertEquals(0, vconfig.vips.size());

        VmNicInventory nic = vm.getVmNics().get(0);
        pfRule1 = api.attachPortForwardingRule(pfRule1.getUuid(), nic.getUuid());
        checkRuleOn(pfRule1, vip);
        Assert.assertEquals(nic.getUuid(), pfRule1.getVmNicUuid());
    }
}
