package org.zstack.test.virtualrouter;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.appliancevm.ApplianceVmVO;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.network.service.portforwarding.PortForwardingRuleInventory;
import org.zstack.network.service.vip.VipInventory;
import org.zstack.network.service.vip.VipVO;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.simulator.virtualrouter.VirtualRouterSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;

/**
 * @author frank
 * @condition 1. create two guest networks
 * 2. only one public in system
 * 3. create two vms with port forwarding rules which maps to two guest networks
 * @test confirm port forwarding rules were applied on 2 virtual router
 */
public class TestVirtualRouterPortForwarding16 {
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
        deployer = new Deployer("deployerXml/virtualRouter/TestVirtualRouterPortForwarding16.xml", con);
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
    public void test() throws ApiSenderException, InterruptedException {
        long count = dbf.count(ApplianceVmVO.class);
        Assert.assertEquals(2, count);

        PortForwardingRuleTestValidator validator = new PortForwardingRuleTestValidator();
        validator.validate(vconfig.portForwardingRules, deployer.portForwardingRules.values());
        PortForwardingRuleInventory pfRule1 = deployer.portForwardingRules.get("pfRule1");
        VipVO vipvo = dbf.findByUuid(pfRule1.getVipUuid(), VipVO.class);
        VipInventory vip = VipInventory.valueOf(vipvo);
        VipTestValidator vipValidator = new VipTestValidator();
        vipValidator.validate(vconfig.vips, vip);

        PortForwardingRuleInventory pfRule2 = deployer.portForwardingRules.get("pfRule2");
        vipvo = dbf.findByUuid(pfRule2.getVipUuid(), VipVO.class);
        vip = VipInventory.valueOf(vipvo);
        vipValidator = new VipTestValidator();
        vipValidator.validate(vconfig.vips, vip);
    }
}
