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
import org.zstack.network.service.portforwarding.PortForwardingRuleInventory;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.simulator.virtualrouter.VirtualRouterSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;

import java.util.List;

/**
 * test APIGetPortForwardingAttachableVmNicsMsg
 *
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

        VmInstanceInventory vm1 = deployer.vms.get("TestVm");
        VmInstanceInventory vm2 = deployer.vms.get("TestVm2");
        VmNicInventory vmNic1 = vm1.getVmNics().get(0);
        VmNicInventory vmNic2 = vm2.getVmNics().get(0);

        List<VmNicInventory> nics = api.getPortForwardingAttachableNics(r1.getUuid());
        Assert.assertEquals(2, nics.size());
        VmNicInventory nic1 = nics.get(0);
        VmNicInventory nic2 = nics.get(1);
        Assert.assertFalse(nic1.getUuid().equals(nic2.getUuid()));

        nics = api.getPortForwardingAttachableNics(r2.getUuid());
        Assert.assertEquals(2, nics.size());
        nic1 = nics.get(0);
        nic2 = nics.get(1);
        Assert.assertFalse(nic1.getUuid().equals(nic2.getUuid()));

        nics = api.getPortForwardingAttachableNics(r3.getUuid());
        Assert.assertEquals(2, nics.size());
        nic1 = nics.get(0);
        nic2 = nics.get(1);
        Assert.assertFalse(nic1.getUuid().equals(nic2.getUuid()));

        api.attachPortForwardingRule(r1.getUuid(), vmNic1.getUuid());

        // rule 2,3 are not attachable to the vm1 because they use other VIPs
        nics = api.getPortForwardingAttachableNics(r2.getUuid());
        Assert.assertEquals(1, nics.size());
        Assert.assertEquals(vmNic2.getUuid(), nics.get(0).getUuid());

        nics = api.getPortForwardingAttachableNics(r3.getUuid());
        Assert.assertEquals(1, nics.size());
        Assert.assertEquals(vmNic2.getUuid(), nics.get(0).getUuid());

        // rule4 share the same vip with rule1, so it's attachable to the vm1
        // and only attachable to the vm1 because the vm2 is on another private L3
        PortForwardingRuleInventory r4 = new PortForwardingRuleInventory();
        r4.setName("rule4");
        r4.setVipUuid(r1.getVipUuid());
        r4.setVipPortStart(200);
        r4.setVipPortEnd(220);
        r4.setPrivatePortStart(200);
        r4.setPrivatePortEnd(220);
        r4.setProtocolType("TCP");
        r4 = api.createPortForwardingRuleByFullConfig(r4);

        nics = api.getPortForwardingAttachableNics(r4.getUuid());
        Assert.assertEquals(1, nics.size());
        Assert.assertEquals(vmNic1.getUuid(), nics.get(0).getUuid());

        // make the vm1 has no pf rules
        api.detachPortForwardingRule(r1.getUuid());
        api.stopVmInstance(vm2.getUuid());
        api.attachPortForwardingRule(r2.getUuid(), vmNic2.getUuid());

        // rule 5 shares the same vip with rule2, as rule2 has been attached to
        // the stopped vm2, the rule5 cannot be attached to the vm1 which is on
        // another guest L3 network
        PortForwardingRuleInventory r5 = new PortForwardingRuleInventory();
        r5.setName("rule5");
        r5.setVipUuid(r2.getVipUuid());
        r5.setVipPortStart(2000);
        r5.setVipPortEnd(2200);
        r5.setPrivatePortStart(2000);
        r5.setPrivatePortEnd(2200);
        r5.setProtocolType("TCP");
        r5 = api.createPortForwardingRuleByFullConfig(r5);

        nics = api.getPortForwardingAttachableNics(r5.getUuid());
        Assert.assertEquals(1, nics.size());
        Assert.assertEquals(vmNic2.getUuid(), nics.get(0).getUuid());
    }
}
