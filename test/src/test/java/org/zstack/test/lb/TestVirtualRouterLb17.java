package org.zstack.test.lb;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.query.QueryOp;
import org.zstack.header.vm.APIQueryVmNicMsg;
import org.zstack.header.vm.APIQueryVmNicReply;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.service.lb.*;
import org.zstack.network.service.vip.VipVO;
import org.zstack.network.service.virtualrouter.APIQueryVirtualRouterVmMsg;
import org.zstack.network.service.virtualrouter.APIQueryVirtualRouterVmReply;
import org.zstack.simulator.appliancevm.ApplianceVmSimulatorConfig;
import org.zstack.simulator.virtualrouter.VirtualRouterSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.search.QueryTestValidator;

/**
 * @author frank
 *         <p>
 *         1. create a lb
 *         2. query
 *         <p>
 *         confirm all queries succeded
 */
public class TestVirtualRouterLb17 {
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
        LoadBalancerVO lbvo = dbf.findByUuid(lb.getUuid(), LoadBalancerVO.class);
        LoadBalancerListenerVO listenerVO = lbvo.getListeners().iterator().next();
        LoadBalancerListenerInventory l = LoadBalancerListenerInventory.valueOf(listenerVO);

        VipVO vip = dbf.findByUuid(lb.getVipUuid(), VipVO.class);
        VmNicInventory nic = deployer.vms.get("TestVm").getVmNics().get(0);

        QueryTestValidator.validateEQ(new APIQueryLoadBalancerMsg(), api, APIQueryLoadBalancerReply.class, lb);
        QueryTestValidator.validateRandomEQConjunction(new APIQueryLoadBalancerMsg(), api, APIQueryLoadBalancerReply.class, lb, 3);

        QueryTestValidator.validateEQ(new APIQueryLoadBalancerListenerMsg(), api, APIQueryLoadBalancerListenerReply.class, l);
        QueryTestValidator.validateRandomEQConjunction(new APIQueryLoadBalancerListenerMsg(), api, APIQueryLoadBalancerListenerReply.class, l, 3);

        APIQueryLoadBalancerMsg msg = new APIQueryLoadBalancerMsg();
        msg.addQueryCondition("vip.ip", QueryOp.EQ, vip.getIp());
        APIQueryLoadBalancerReply reply = api.query(msg, APIQueryLoadBalancerReply.class);
        Assert.assertEquals(1, reply.getInventories().size());
        LoadBalancerInventory lbr = reply.getInventories().get(0);
        Assert.assertEquals(lb.getUuid(), lbr.getUuid());

        APIQueryLoadBalancerListenerMsg lmsg = new APIQueryLoadBalancerListenerMsg();
        lmsg.addQueryCondition("vmNic.uuid", QueryOp.EQ, nic.getUuid());
        APIQueryLoadBalancerListenerReply lreply = api.query(lmsg, APIQueryLoadBalancerListenerReply.class);
        Assert.assertEquals(1, lreply.getInventories().size());
        LoadBalancerListenerInventory lr = lreply.getInventories().get(0);
        Assert.assertEquals(l.getUuid(), lr.getUuid());

        APIQueryVmNicMsg nmsg = new APIQueryVmNicMsg();
        nmsg.addQueryCondition("loadBalancerListener.loadBalancer.vip.ip", QueryOp.EQ, vip.getIp());
        APIQueryVmNicReply nreply = api.query(nmsg, APIQueryVmNicReply.class);
        Assert.assertEquals(1, nreply.getInventories().size());
        VmNicInventory rnic = nreply.getInventories().get(0);
        Assert.assertEquals(nic.getUuid(), rnic.getUuid());

        APIQueryVirtualRouterVmMsg vmsg = new APIQueryVirtualRouterVmMsg();
        vmsg.addQueryCondition("loadBalancer.uuid", QueryOp.EQ, lb.getUuid());
        APIQueryVirtualRouterVmReply vreply = api.query(vmsg, APIQueryVirtualRouterVmReply.class);
        Assert.assertEquals(1, vreply.getInventories().size());
    }
}
