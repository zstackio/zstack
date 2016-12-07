package org.zstack.test.lb;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.service.lb.LoadBalancerListenerInventory;
import org.zstack.simulator.appliancevm.ApplianceVmSimulatorConfig;
import org.zstack.simulator.virtualrouter.VirtualRouterSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;

import java.util.List;

/**
 *
 * test APIGetVmNicCandidatesForLoadBalancerMsg
 */
public class TestVirtualRouterLb20 {
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
        deployer = new Deployer("deployerXml/lb/TestVirtualRouterLb20.xml", con);
        deployer.addSpringConfig("VirtualRouter.xml");
        deployer.addSpringConfig("VirtualRouterSimulator.xml");
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("vip.xml");
        deployer.addSpringConfig("lb.xml");
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
        LoadBalancerListenerInventory l1 = deployer.loadBalancerListeners.get("listener1");
        VmInstanceInventory vm1 = deployer.vms.get("TestVm");
        VmNicInventory nic1 = vm1.getVmNics().get(0);

        LoadBalancerListenerInventory l2 = deployer.loadBalancerListeners.get("listener2");
        VmInstanceInventory vm2 = deployer.vms.get("TestVm1");
        VmNicInventory nic2 = vm2.getVmNics().get(0);

        List<VmNicInventory> nics = api.getVmNicCandidatesForLoadBalancer(l1.getUuid(), null);
        Assert.assertEquals(1, nics.size());
        VmNicInventory nic = nics.get(0);
        Assert.assertEquals(nic2.getUuid(), nic.getUuid());

        nics = api.getVmNicCandidatesForLoadBalancer(l2.getUuid(), null);
        Assert.assertEquals(2, nics.size());
        VmNicInventory nic11 = nics.get(0);
        VmNicInventory nic22 = nics.get(1);
        Assert.assertFalse(nic11.getUuid().equals(nic22.getUuid()));

        api.removeNicFromLoadBalancerListener(l1.getUuid(), nic1.getUuid(), null);
        nics = api.getVmNicCandidatesForLoadBalancer(l1.getUuid(), null);
        Assert.assertEquals(2, nics.size());
        nic11 = nics.get(0);
        nic22 = nics.get(1);
        Assert.assertFalse(nic11.getUuid().equals(nic22.getUuid()));

        nics = api.getVmNicCandidatesForLoadBalancer(l2.getUuid(), null);
        Assert.assertEquals(2, nics.size());
        nic11 = nics.get(0);
        nic22 = nics.get(1);
        Assert.assertFalse(nic11.getUuid().equals(nic22.getUuid()));

        api.addVmNicToLoadBalancerListener(l1.getUuid(), nic1.getUuid());
        api.addVmNicToLoadBalancerListener(l1.getUuid(), nic2.getUuid());

        nics = api.getVmNicCandidatesForLoadBalancer(l1.getUuid(), null);
        Assert.assertEquals(0, nics.size());
        nics = api.getVmNicCandidatesForLoadBalancer(l2.getUuid(), null);
        Assert.assertEquals(2, nics.size());
    }
}
