package org.zstack.test.lb;

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
import org.zstack.network.service.lb.LoadBalancerListenerInventory;
import org.zstack.network.service.virtualrouter.VirtualRouterRoleManager;
import org.zstack.network.service.virtualrouter.VirtualRouterSystemTags;
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO;
import org.zstack.network.service.virtualrouter.lb.VirtualRouterLoadBalancerRefVO;
import org.zstack.simulator.appliancevm.ApplianceVmSimulatorConfig;
import org.zstack.simulator.virtualrouter.VirtualRouterSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;

import java.util.List;

/**
 * @author frank
 * @condition 1. create a lb
 * 2. use separate vr
 * 3. stop the vm
 * 4. destroy the separate vr
 * 5, start the vm
 * @test confirm the separate vr is re-created
 */
public class TestVirtualRouterLb10 {
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
        deployer = new Deployer("deployerXml/lb/TestVirtualRouterLb2.xml", con);
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
        L3NetworkInventory gnw = deployer.l3Networks.get("GuestNetwork");
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        VmNicInventory nic = vm.findNic(gnw.getUuid());
        LoadBalancerListenerInventory l = deployer.loadBalancerListeners.get("listener");
        api.addVmNicToLoadBalancerListener(l.getUuid(), nic.getUuid());

        Assert.assertEquals(2, dbf.count(VirtualRouterVmVO.class));
        Assert.assertEquals(1, dbf.count(VirtualRouterLoadBalancerRefVO.class));
        VirtualRouterLoadBalancerRefVO ref = dbf.listAll(VirtualRouterLoadBalancerRefVO.class).get(0);
        VirtualRouterVmVO vr = dbf.findByUuid(ref.getVirtualRouterVmUuid(), VirtualRouterVmVO.class);
        List<String> roles = new VirtualRouterRoleManager().getAllRoles(vr.getUuid());
        Assert.assertEquals(1, roles.size());
        Assert.assertTrue(roles.contains(VirtualRouterSystemTags.VR_LB_ROLE.getTagFormat()));

        api.stopVmInstance(vm.getUuid());
        api.startVmInstance(vm.getUuid());
        vr = dbf.findByUuid(ref.getVirtualRouterVmUuid(), VirtualRouterVmVO.class);
        Assert.assertNotNull(vr);
    }
}
