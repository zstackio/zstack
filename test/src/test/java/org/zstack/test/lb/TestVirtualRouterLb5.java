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
import org.zstack.network.service.lb.LoadBalancerInventory;
import org.zstack.network.service.lb.LoadBalancerListenerInventory;
import org.zstack.network.service.lb.LoadBalancerListenerVO;
import org.zstack.network.service.lb.LoadBalancerVO;
import org.zstack.network.service.virtualrouter.lb.VirtualRouterLoadBalancerBackend.LbTO;
import org.zstack.network.service.virtualrouter.lb.VirtualRouterLoadBalancerBackend.RefreshLbCmd;
import org.zstack.simulator.appliancevm.ApplianceVmSimulatorConfig;
import org.zstack.simulator.virtualrouter.VirtualRouterSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.function.Function;

/**
 * @author frank
 *         <p>
 *         1. create a lb
 *         2. add a listener
 *         3. add a nic to the listener
 *         <p>
 *         confirm the listener added successfully
 *         <p>
 *         3. remove the listener
 *         <p>
 *         confirm the listener removed successfully
 *         <p>
 *         4. add the listener again
 *         5. make adding nic fail
 *         6. add a nic to the listener
 *         <p>
 *         confirm the nic failed to add
 */
public class TestVirtualRouterLb5 {
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
        deployer = new Deployer("deployerXml/lb/TestVirtualRouterLb.xml", con);
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
        LoadBalancerInventory lb = deployer.loadBalancers.get("lb");
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        VmNicInventory nic = vm.getVmNics().get(0);

        vconfig.refreshLbCmds.clear();
        LoadBalancerListenerInventory listener1 = new LoadBalancerListenerInventory();
        listener1.setName("test");
        listener1.setLoadBalancerPort(100);
        listener1.setInstancePort(100);
        listener1.setLoadBalancerUuid(lb.getUuid());
        listener1.setProtocol("http");
        listener1 = api.createLoadBalancerListener(listener1, null);
        LoadBalancerVO lbvo = dbf.findByUuid(lb.getUuid(), LoadBalancerVO.class);
        Assert.assertEquals(2, lbvo.getListeners().size());
        final LoadBalancerListenerInventory finalListener = listener1;
        LoadBalancerListenerVO listenerVO = CollectionUtils.find(lbvo.getListeners(), new Function<LoadBalancerListenerVO, LoadBalancerListenerVO>() {
            @Override
            public LoadBalancerListenerVO call(LoadBalancerListenerVO arg) {
                return arg.getInstancePort() == finalListener.getInstancePort() ? arg : null;
            }
        });
        Assert.assertNotNull(listenerVO);
        Assert.assertTrue(vconfig.refreshLbCmds.isEmpty());

        api.addVmNicToLoadBalancerListener(listener1.getUuid(), nic.getUuid());
        Assert.assertEquals(100, listenerVO.getLoadBalancerPort());
        Assert.assertEquals(100, listenerVO.getInstancePort());
        Assert.assertEquals("http", listenerVO.getProtocol());
        Assert.assertFalse(vconfig.refreshLbCmds.isEmpty());
        RefreshLbCmd cmd = vconfig.refreshLbCmds.get(0);
        Assert.assertEquals(2, cmd.getLbs().size());
        LbTO to = CollectionUtils.find(cmd.getLbs(), new Function<LbTO, LbTO>() {
            @Override
            public LbTO call(LbTO arg) {
                return arg.getInstancePort() == 100 ? arg : null;
            }
        });
        Assert.assertNotNull(to);
        Assert.assertEquals(listener1.getProtocol(), to.getMode());
        Assert.assertEquals(listener1.getInstancePort(), to.getInstancePort());
        Assert.assertEquals(listener1.getLoadBalancerPort(), to.getLoadBalancerPort());
        Assert.assertFalse(to.getNicIps().isEmpty());
        String nicIp = to.getNicIps().get(0);
        Assert.assertEquals(nic.getIp(), nicIp);

        vconfig.refreshLbCmds.clear();
        api.deleteLoadBalancerListener(listener1.getUuid(), null);
        lbvo = dbf.findByUuid(lb.getUuid(), LoadBalancerVO.class);
        Assert.assertEquals(1, lbvo.getListeners().size());
        final LoadBalancerListenerInventory finalListener1 = listener1;
        listenerVO = CollectionUtils.find(lbvo.getListeners(), new Function<LoadBalancerListenerVO, LoadBalancerListenerVO>() {
            @Override
            public LoadBalancerListenerVO call(LoadBalancerListenerVO arg) {
                return arg.getInstancePort() == finalListener1.getInstancePort() ? arg : null;
            }
        });
        Assert.assertNull(listenerVO);
        Assert.assertFalse(vconfig.refreshLbCmds.isEmpty());
        cmd = vconfig.refreshLbCmds.get(0);
        Assert.assertEquals(2, cmd.getLbs().size());
        to = CollectionUtils.find(cmd.getLbs(), new Function<LbTO, LbTO>() {
            @Override
            public LbTO call(LbTO arg) {
                return arg.getInstancePort() == 100 ? arg : null;
            }
        });
        Assert.assertNotNull(to);

        listener1 = api.createLoadBalancerListener(listener1, null);
        vconfig.refreshLbSuccess = false;
        boolean s = false;
        try {
            api.addVmNicToLoadBalancerListener(listener1.getUuid(), nic.getUuid());
        } catch (ApiSenderException e) {
            s = true;
        }
        Assert.assertTrue(s);
        LoadBalancerListenerVO lvo = dbf.findByUuid(listener1.getUuid(), LoadBalancerListenerVO.class);
        Assert.assertTrue(lvo.getVmNicRefs().isEmpty());
    }
}
