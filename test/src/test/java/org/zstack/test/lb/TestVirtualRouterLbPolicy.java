package org.zstack.test.lb;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.AccountConstant.StatementEffect;
import org.zstack.header.identity.*;
import org.zstack.header.identity.PolicyInventory.Statement;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.query.QueryCondition;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.service.lb.*;
import org.zstack.network.service.vip.VipInventory;
import org.zstack.simulator.appliancevm.ApplianceVmSimulatorConfig;
import org.zstack.simulator.virtualrouter.VirtualRouterSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.identity.IdentityCreator;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.function.Function;

import java.util.ArrayList;
import java.util.List;

/**
 * @author frank
 * @condition 1. create a lb
 * @test confirm lb are created successfully
 */
public class TestVirtualRouterLbPolicy {
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
        deployer = new Deployer("deployerXml/lb/TestVirtualRouterLbPolicy.xml", con);
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
        IdentityCreator identityCreator = new IdentityCreator(api);
        AccountInventory test = identityCreator.useAccount("test");
        SessionInventory session = identityCreator.getAccountSession();
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        VmNicInventory nic = vm.getVmNics().get(0);

        L3NetworkInventory pubNw = deployer.l3Networks.get("PublicNetwork");
        VipInventory vip = api.acquireIp(pubNw.getUuid(), session);
        LoadBalancerInventory lb = api.createLoadBalancer("lb", vip.getUuid(), null, session);

        List<Quota.QuotaUsage> usages = api.getQuotaUsage(test.getUuid(), null);
        Quota.QuotaUsage u = CollectionUtils.find(usages, new Function<Quota.QuotaUsage, Quota.QuotaUsage>() {
            @Override
            public Quota.QuotaUsage call(Quota.QuotaUsage arg) {
                return arg.getName().equals(LoadBalancerConstants.QUOTA_LOAD_BALANCER_NUM) ? arg : null;
            }
        });
        Assert.assertNotNull(u);
        QuotaInventory lbquota = api.getQuota(LoadBalancerConstants.QUOTA_LOAD_BALANCER_NUM, test.getUuid(), session);
        Assert.assertEquals(lbquota.getValue(), u.getTotal().longValue());
        Assert.assertEquals(1, u.getUsed().longValue());

        identityCreator.createUser("user1", "password");
        Statement s = new Statement();
        s.setName("allow");
        s.setEffect(StatementEffect.Allow);
        s.addAction(String.format("%s:%s", LoadBalancerConstants.ACTION_CATEGORY, APICreateLoadBalancerMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", LoadBalancerConstants.ACTION_CATEGORY, APICreateLoadBalancerListenerMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", LoadBalancerConstants.ACTION_CATEGORY, APIAddVmNicToLoadBalancerMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", LoadBalancerConstants.ACTION_CATEGORY, APIRemoveVmNicFromLoadBalancerMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", LoadBalancerConstants.ACTION_CATEGORY, APIDeleteLoadBalancerListenerMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", LoadBalancerConstants.ACTION_CATEGORY, APIDeleteLoadBalancerMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", LoadBalancerConstants.ACTION_CATEGORY, APIRefreshLoadBalancerMsg.class.getSimpleName()));
        identityCreator.createPolicy("allow", s);
        identityCreator.attachPolicyToUser("user1", "allow");
        SessionInventory userSession = identityCreator.userLogin("user1", "password");

        LoadBalancerListenerInventory listener = new LoadBalancerListenerInventory();
        listener.setUuid(Platform.getUuid());
        listener.setName("listener");
        listener.setLoadBalancerPort(80);
        listener.setInstancePort(90);
        listener.setLoadBalancerUuid(lb.getUuid());
        listener.setProtocol("http");

        listener = api.createLoadBalancerListener(listener, userSession);
        api.addVmNicToLoadBalancerListener(listener.getUuid(), nic.getUuid(), userSession);
        api.refreshLoadBalancer(lb.getUuid(), userSession);
        api.removeNicFromLoadBalancerListener(listener.getUuid(), nic.getUuid(), userSession);
        api.deleteLoadBalancerListener(listener.getUuid(), userSession);
        api.deleteLoadBalancer(lb.getUuid(), session);

        // test deny
        lb = api.createLoadBalancer("lb", vip.getUuid(), null, userSession);
        listener.setLoadBalancerUuid(lb.getUuid());
        listener = api.createLoadBalancerListener(listener, userSession);
        s.setName("deny");
        s.setEffect(StatementEffect.Deny);
        s.addAction(String.format("%s:%s", LoadBalancerConstants.ACTION_CATEGORY, APICreateLoadBalancerMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", LoadBalancerConstants.ACTION_CATEGORY, APICreateLoadBalancerListenerMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", LoadBalancerConstants.ACTION_CATEGORY, APIAddVmNicToLoadBalancerMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", LoadBalancerConstants.ACTION_CATEGORY, APIRemoveVmNicFromLoadBalancerMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", LoadBalancerConstants.ACTION_CATEGORY, APIDeleteLoadBalancerListenerMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", LoadBalancerConstants.ACTION_CATEGORY, APIDeleteLoadBalancerMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", LoadBalancerConstants.ACTION_CATEGORY, APIRefreshLoadBalancerMsg.class.getSimpleName()));
        identityCreator.createPolicy("deny", s);
        identityCreator.detachPolicyFromUser("user1", "allow");
        identityCreator.attachPolicyToUser("user1", "deny");

        boolean ss = false;
        try {
            api.addVmNicToLoadBalancerListener(listener.getUuid(), nic.getUuid(), userSession);
        } catch (ApiSenderException e) {
            if (e.getError().getCode().equals(IdentityErrors.PERMISSION_DENIED.toString())) {
                ss = true;
            }
        }
        Assert.assertTrue(ss);

        ss = false;
        try {
            api.removeNicFromLoadBalancerListener(listener.getUuid(), nic.getUuid(), userSession);
        } catch (ApiSenderException e) {
            if (e.getError().getCode().equals(IdentityErrors.PERMISSION_DENIED.toString())) {
                ss = true;
            }
        }
        Assert.assertTrue(ss);

        ss = false;
        try {
            api.createLoadBalancerListener(listener, userSession);
        } catch (ApiSenderException e) {
            if (e.getError().getCode().equals(IdentityErrors.PERMISSION_DENIED.toString())) {
                ss = true;
            }
        }
        Assert.assertTrue(ss);

        ss = false;
        try {
            api.deleteLoadBalancerListener(listener.getUuid(), userSession);
        } catch (ApiSenderException e) {
            if (e.getError().getCode().equals(IdentityErrors.PERMISSION_DENIED.toString())) {
                ss = true;
            }
        }
        Assert.assertTrue(ss);
        api.deleteLoadBalancerListener(listener.getUuid(), session);

        ss = false;
        try {
            api.deleteLoadBalancer(lb.getUuid(), userSession);
        } catch (ApiSenderException e) {
            if (e.getError().getCode().equals(IdentityErrors.PERMISSION_DENIED.toString())) {
                ss = true;
            }
        }
        Assert.assertTrue(ss);

        // test quota
        api.deleteLoadBalancer(lb.getUuid(), session);
        api.updateQuota(test.getUuid(), LoadBalancerConstants.QUOTA_LOAD_BALANCER_NUM, 0);
        ss = false;
        try {
            api.createLoadBalancer("lb", vip.getUuid(), null, session);
        } catch (ApiSenderException e) {
            if (e.getError().getCode().equals(IdentityErrors.QUOTA_EXCEEDING.toString())) {
                ss = true;
            }
        }
        Assert.assertTrue(ss);

        api.updateQuota(test.getUuid(), LoadBalancerConstants.QUOTA_LOAD_BALANCER_NUM, 5);
        api.createLoadBalancer("lb", vip.getUuid(), null, session);

        // test query
        APIQueryLoadBalancerMsg qlmsg = new APIQueryLoadBalancerMsg();
        qlmsg.setConditions(new ArrayList<QueryCondition>());
        api.query(qlmsg, APIQueryLoadBalancerReply.class, userSession);

        APIQueryLoadBalancerListenerMsg qmsg = new APIQueryLoadBalancerListenerMsg();
        qmsg.setConditions(new ArrayList<QueryCondition>());
        api.query(qmsg, APIQueryLoadBalancerListenerReply.class, userSession);
    }
}
