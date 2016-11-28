package org.zstack.test.lb;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.service.lb.LoadBalancerInventory;
import org.zstack.network.service.lb.LoadBalancerListenerVmNicRefVO;
import org.zstack.network.service.lb.LoadBalancerListenerVmNicRefVO_;
import org.zstack.network.service.lb.LoadBalancerVmNicStatus;
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
 *         2. stop the vm
 *         <p>
 *         confirm the nic is inactive
 *         <p>
 *         3. start the vm
 *         <p>
 *         confirm the nic is active
 */
public class TestVirtualRouterLb9 {
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
        L3NetworkInventory gnw = deployer.l3Networks.get("GuestNetwork");
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        final VmNicInventory nic = vm.findNic(gnw.getUuid());

        vconfig.refreshLbCmds.clear();
        api.stopVmInstance(vm.getUuid());
        Assert.assertFalse(vconfig.refreshLbCmds.isEmpty());
        RefreshLbCmd cmd = vconfig.refreshLbCmds.get(0);
        String ip = CollectionUtils.find(cmd.getLbs(), new Function<String, LbTO>() {
            @Override
            public String call(LbTO arg) {
                return arg.getNicIps().contains(nic.getIp()) ? nic.getIp() : null;
            }
        });
        Assert.assertNull(ip);

        SimpleQuery<LoadBalancerListenerVmNicRefVO> q = dbf.createQuery(LoadBalancerListenerVmNicRefVO.class);
        q.add(LoadBalancerListenerVmNicRefVO_.vmNicUuid, Op.EQ, nic.getUuid());
        LoadBalancerListenerVmNicRefVO ref = q.find();
        Assert.assertNotNull(ref);
        Assert.assertEquals(LoadBalancerVmNicStatus.Inactive, ref.getStatus());

        vconfig.refreshLbCmds.clear();
        api.startVmInstance(vm.getUuid());
        Assert.assertFalse(vconfig.refreshLbCmds.isEmpty());
        cmd = vconfig.refreshLbCmds.get(0);
        ip = CollectionUtils.find(cmd.getLbs(), new Function<String, LbTO>() {
            @Override
            public String call(LbTO arg) {
                return arg.getNicIps().contains(nic.getIp()) ? nic.getIp() : null;
            }
        });
        Assert.assertNotNull(ip);
        ref = q.find();
        Assert.assertNotNull(ref);
        Assert.assertEquals(LoadBalancerVmNicStatus.Active, ref.getStatus());
    }
}
