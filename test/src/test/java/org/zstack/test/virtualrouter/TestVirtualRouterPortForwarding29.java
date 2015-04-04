package org.zstack.test.virtualrouter;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.VmNicInventory;
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

import java.util.List;

/**
 * 
 * @author frank
 * 
 * @condition
 * 1. get attachable vm nics for rule
 *
 * @test
 * confirm state change success
 */
public class TestVirtualRouterPortForwarding29 {
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
        deployer = new Deployer("deployerXml/virtualRouter/TestVirtualRouterPortForwarding29.xml", con);
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
        PortForwardingRuleInventory r1 = deployer.portForwardingRules.get("pfRule1");
        PortForwardingRuleInventory r2 = deployer.portForwardingRules.get("pfRule2");
        PortForwardingRuleInventory r3 = deployer.portForwardingRules.get("pfRule3");

        L3NetworkInventory guestL3 = deployer.l3Networks.get("GuestNetwork");

        List<VmNicInventory> nics = api.getPortForwardingAttachableNics(r1.getUuid());
        Assert.assertEquals(1, nics.size());
        VmNicInventory nic = nics.get(0);
        Assert.assertEquals(guestL3.getUuid(), nic.getL3NetworkUuid());

        nics = api.getPortForwardingAttachableNics(r2.getUuid());
        Assert.assertEquals(1, nics.size());
        nic = nics.get(0);
        Assert.assertEquals(guestL3.getUuid(), nic.getL3NetworkUuid());

        nics = api.getPortForwardingAttachableNics(r3.getUuid());
        Assert.assertEquals(1, nics.size());
        nic = nics.get(0);
        Assert.assertEquals(guestL3.getUuid(), nic.getL3NetworkUuid());

        api.attachPortForwardingRule(r1.getUuid(), nic.getUuid());

        nics = api.getPortForwardingAttachableNics(r2.getUuid());
        Assert.assertEquals(0, nics.size());

        nics = api.getPortForwardingAttachableNics(r3.getUuid());
        Assert.assertEquals(1, nics.size());
        nic = nics.get(0);
        Assert.assertEquals(guestL3.getUuid(), nic.getL3NetworkUuid());
    }
}
