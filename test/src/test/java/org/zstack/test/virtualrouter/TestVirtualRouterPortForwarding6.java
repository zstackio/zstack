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
import org.zstack.network.service.virtualrouter.portforwarding.PortForwardingRuleTO;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.simulator.virtualrouter.VirtualRouterSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author frank
 * @condition 1. create a vm with port forwarding rule using deployer
 * 2. stop virtual router
 * 3. start virtual router
 * @test confirm port forwarding rule was applied on virtual router
 */
public class TestVirtualRouterPortForwarding6 {
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
    public void test() throws ApiSenderException, InterruptedException {
        List<ApplianceVmVO> vrs = dbf.listAll(ApplianceVmVO.class);
        ApplianceVmVO vr = vrs.get(0);
        PortForwardingRuleInventory pfRule1 = deployer.portForwardingRules.get("pfRule1");
        api.stopVmInstance(vr.getUuid());
        vconfig.portForwardingRules.clear();
        vconfig.vips.clear();
        api.startVmInstance(vr.getUuid());
        // though appliance vm has started, we need to wait virtual router post start process complete
        TimeUnit.SECONDS.sleep(2);
        PortForwardingRuleTO to = vconfig.portForwardingRules.get(0);
        PortForwardingRuleTestValidator validator = new PortForwardingRuleTestValidator();
        Assert.assertTrue(validator.compare(to, pfRule1));

    }
}
