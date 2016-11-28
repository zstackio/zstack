package org.zstack.test.virtualrouter;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.config.GlobalConfigFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.network.service.portforwarding.PortForwardingConstant;
import org.zstack.network.service.portforwarding.PortForwardingProtocolType;
import org.zstack.network.service.portforwarding.PortForwardingRuleInventory;
import org.zstack.network.service.portforwarding.PortForwardingRuleVO;
import org.zstack.network.service.vip.VipInventory;
import org.zstack.network.service.vip.VipVO;
import org.zstack.network.service.virtualrouter.portforwarding.PortForwardingRuleTO;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.simulator.virtualrouter.VirtualRouterSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;

import java.util.List;

/**
 * @author frank
 * @condition 1. create a vm
 * 2. acquire a public ip: pub1
 * 3. set port forwarding to vm using pub1
 * 4. destroy vm
 * @test confirm rule has been removed
 */
public class TestVirtualRouterPortForwarding13 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    VirtualRouterSimulatorConfig vconfig;
    KVMSimulatorConfig kconfig;
    GlobalConfigFacade gcf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/virtualRouter/TestVirtualRouterPortForwarding.xml", con);
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
        gcf = loader.getComponent(GlobalConfigFacade.class);
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
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

        api.destroyVmInstance(vm.getUuid());
        List<PortForwardingRuleInventory> rules = api.listPortForwardingRules(null);
        Assert.assertFalse(rules.isEmpty());
        PortForwardingRuleVO pfvo = dbf.findByUuid(rule.getUuid(), PortForwardingRuleVO.class);
        Assert.assertEquals(pfvo.getVipUuid(), vip.getUuid());
        Assert.assertNull(pfvo.getVmNicUuid());
        VipVO vipvo = dbf.findByUuid(vip.getUuid(), VipVO.class);
        Assert.assertEquals(PortForwardingConstant.PORTFORWARDING_NETWORK_SERVICE_TYPE, vipvo.getUseFor());
    }
}
