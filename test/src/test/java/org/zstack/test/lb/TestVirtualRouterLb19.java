package org.zstack.test.lb;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.service.lb.LoadBalancerListenerInventory;
import org.zstack.network.service.lb.LoadBalancerListenerVmNicRefVO;
import org.zstack.network.service.lb.LoadBalancerListenerVmNicRefVO_;
import org.zstack.simulator.appliancevm.ApplianceVmSimulatorConfig;
import org.zstack.simulator.virtualrouter.VirtualRouterSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;

/**
 *
 * 1. add a vm nic to two listeners
 * 2. remove the nic from on listener
 *
 * confirm the nic is still in another listener
 * for BUG: https://github.com/zxwing/premium/issues/1478
 *
 */
public class TestVirtualRouterLb19 {
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
        deployer = new Deployer("deployerXml/lb/TestVirtualRouterLb19.xml", con);
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
        LoadBalancerListenerInventory l = deployer.loadBalancerListeners.get("listener1");
        VmInstanceInventory vm = deployer.vms.get("TestVm");

        VmNicInventory nic = vm.getVmNics().get(0);
        api.removeNicFromLoadBalancerListener(l.getUuid(), nic.getUuid(), null);

        LoadBalancerListenerInventory l2 = deployer.loadBalancerListeners.get("listener2");
        SimpleQuery<LoadBalancerListenerVmNicRefVO> q = dbf.createQuery(LoadBalancerListenerVmNicRefVO.class);
        q.add(LoadBalancerListenerVmNicRefVO_.listenerUuid, SimpleQuery.Op.EQ, l2.getUuid());
        q.add(LoadBalancerListenerVmNicRefVO_.vmNicUuid, SimpleQuery.Op.EQ, nic.getUuid());
        Assert.assertTrue(q.isExists());
    }
}
