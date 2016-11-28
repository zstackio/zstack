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
import org.zstack.network.service.portforwarding.PortForwardingProtocolType;
import org.zstack.network.service.portforwarding.PortForwardingRuleInventory;
import org.zstack.network.service.vip.VipInventory;
import org.zstack.simulator.appliancevm.ApplianceVmSimulatorConfig;
import org.zstack.simulator.virtualrouter.VirtualRouterSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;

public class TestUpdateVirtualRouterPortForwardingAndVip {
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

        vip.setName("1");
        vip.setDescription("xxx");
        vip = api.updateVip(vip);

        Assert.assertEquals("1", vip.getName());
        Assert.assertEquals("xxx", vip.getDescription());

        rule.setName("1");
        rule.setDescription("xxx");
        rule = api.updatePortForwardingRule(rule);
        Assert.assertEquals("1", rule.getName());
        Assert.assertEquals("xxx", rule.getDescription());
    }
}
