package org.zstack.test.lb;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.service.lb.*;
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
import org.zstack.utils.gson.JSONObjectUtil;

import static org.zstack.utils.CollectionDSL.*;

/**
 * @author frank
 *         <p>
 *         1. create a lb
 *         2. add a listener with a nic
 *         <p>
 *         confirm the system tags are created as default
 *         <p>
 *         3. remove the listener
 *         <p>
 *         confirm the system tags are removed
 *         <p>
 *         4. add the listener again with system tags specified with a nic
 *         <p>
 *         confirm the system tags are created as specified
 */
public class TestVirtualRouterLb13 {
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
        VmNicInventory nic = deployer.vms.get("TestVm").getVmNics().get(0);

        vconfig.refreshLbCmds.clear();
        LoadBalancerListenerInventory listener1 = new LoadBalancerListenerInventory();
        listener1.setName("test");
        listener1.setLoadBalancerPort(100);
        listener1.setInstancePort(100);
        listener1.setLoadBalancerUuid(lb.getUuid());
        listener1.setProtocol("http");
        final LoadBalancerListenerInventory l = api.createLoadBalancerListener(listener1, null);
        LoadBalancerVO lbvo = dbf.findByUuid(lb.getUuid(), LoadBalancerVO.class);
        Assert.assertEquals(2, lbvo.getListeners().size());
        LoadBalancerListenerVO listenerVO = CollectionUtils.find(lbvo.getListeners(), new Function<LoadBalancerListenerVO, LoadBalancerListenerVO>() {
            @Override
            public LoadBalancerListenerVO call(LoadBalancerListenerVO arg) {
                return arg.getInstancePort() == l.getInstancePort() ? arg : null;
            }
        });
        Assert.assertNotNull(listenerVO);
        api.addVmNicToLoadBalancerListener(l.getUuid(), nic.getUuid());

        RefreshLbCmd cmd = vconfig.refreshLbCmds.get(0);
        LbTO to = cmd.getLbs().get(0);

        final String s1 = LoadBalancerSystemTags.CONNECTION_IDLE_TIMEOUT.getTokenByResourceUuid(l.getUuid(), LoadBalancerSystemTags.CONNECTION_IDLE_TIMEOUT_TOKEN);
        Long val = Long.valueOf(s1);
        Assert.assertEquals(val, LoadBalancerGlobalConfig.CONNECTION_IDLE_TIMEOUT.value(Long.class));
        String tval = CollectionUtils.find(to.getParameters(), new Function<String, String>() {
            @Override
            public String call(String arg) {
                return arg.equals(LoadBalancerSystemTags.CONNECTION_IDLE_TIMEOUT.getTag(l.getUuid())) ? arg : null;
            }
        });
        Assert.assertNotNull(tval);

        final String s2 = LoadBalancerSystemTags.HEALTH_INTERVAL.getTokenByResourceUuid(l.getUuid(), LoadBalancerSystemTags.HEALTH_INTERVAL_TOKEN);
        val = Long.valueOf(s2);
        Assert.assertEquals(val, LoadBalancerGlobalConfig.HEALTH_INTERVAL.value(Long.class));
        tval = CollectionUtils.find(to.getParameters(), new Function<String, String>() {
            @Override
            public String call(String arg) {
                return arg.equals(LoadBalancerSystemTags.HEALTH_INTERVAL.getTag(l.getUuid())) ? arg : null;
            }
        });
        Assert.assertNotNull(tval);

        final String s3 = LoadBalancerSystemTags.HEALTHY_THRESHOLD.getTokenByResourceUuid(l.getUuid(), LoadBalancerSystemTags.HEALTHY_THRESHOLD_TOKEN);
        val = Long.valueOf(s3);
        Assert.assertEquals(val, LoadBalancerGlobalConfig.HEALTHY_THRESHOLD.value(Long.class));
        tval = CollectionUtils.find(to.getParameters(), new Function<String, String>() {
            @Override
            public String call(String arg) {
                return arg.equals(LoadBalancerSystemTags.HEALTHY_THRESHOLD.getTag(l.getUuid())) ? arg : null;
            }
        });
        Assert.assertNotNull(tval);

        final String s4 = LoadBalancerSystemTags.UNHEALTHY_THRESHOLD.getTokenByResourceUuid(l.getUuid(), LoadBalancerSystemTags.UNHEALTHY_THRESHOLD_TOKEN);
        val = Long.valueOf(s4);
        Assert.assertEquals(val, LoadBalancerGlobalConfig.UNHEALTHY_THRESHOLD.value(Long.class));
        tval = CollectionUtils.find(to.getParameters(), new Function<String, String>() {
            @Override
            public String call(String arg) {
                return arg.equals(LoadBalancerSystemTags.UNHEALTHY_THRESHOLD.getTag(l.getUuid())) ? arg : null;
            }
        });
        Assert.assertNotNull(tval);

        final String s5 = LoadBalancerSystemTags.HEALTH_TIMEOUT.getTokenByResourceUuid(l.getUuid(), LoadBalancerSystemTags.HEALTH_TIMEOUT_TOKEN);
        val = Long.valueOf(s5);
        Assert.assertEquals(val, LoadBalancerGlobalConfig.HEALTH_TIMEOUT.value(Long.class));
        tval = CollectionUtils.find(to.getParameters(), new Function<String, String>() {
            @Override
            public String call(String arg) {
                return arg.equals(LoadBalancerSystemTags.HEALTH_TIMEOUT.getTag(l.getUuid())) ? arg : null;
            }
        });
        Assert.assertNotNull(tval);

        final String s6 = LoadBalancerSystemTags.MAX_CONNECTION.getTokenByResourceUuid(l.getUuid(), LoadBalancerSystemTags.MAX_CONNECTION_TOKEN);
        val = Long.valueOf(s6);
        Assert.assertEquals(val, LoadBalancerGlobalConfig.MAX_CONNECTION.value(Long.class));
        tval = CollectionUtils.find(to.getParameters(), new Function<String, String>() {
            @Override
            public String call(String arg) {
                return arg.equals(LoadBalancerSystemTags.MAX_CONNECTION.getTag(l.getUuid())) ? arg : null;
            }
        });
        Assert.assertNotNull(tval);

        final String s7 = LoadBalancerSystemTags.BALANCER_ALGORITHM.getTokenByResourceUuid(l.getUuid(), LoadBalancerSystemTags.BALANCER_ALGORITHM_TOKEN);
        Assert.assertEquals(s7, LoadBalancerGlobalConfig.BALANCER_ALGORITHM.value());
        tval = CollectionUtils.find(to.getParameters(), new Function<String, String>() {
            @Override
            public String call(String arg) {
                return arg.equals(LoadBalancerSystemTags.BALANCER_ALGORITHM.getTag(l.getUuid())) ? arg : null;
            }
        });
        Assert.assertNotNull(tval);

        final String s8 = LoadBalancerSystemTags.HEALTH_TARGET.getTokenByResourceUuid(l.getUuid(), LoadBalancerSystemTags.HEALTH_TARGET_TOKEN);
        Assert.assertEquals(s8, LoadBalancerGlobalConfig.HEALTH_TARGET.value());
        tval = CollectionUtils.find(to.getParameters(), new Function<String, String>() {
            @Override
            public String call(String arg) {
                return arg.equals(LoadBalancerSystemTags.HEALTH_TARGET.getTag(l.getUuid())) ? arg : null;
            }
        });
        Assert.assertNotNull(tval);

        api.deleteLoadBalancerListener(l.getUuid(), null);
        Assert.assertFalse(LoadBalancerSystemTags.CONNECTION_IDLE_TIMEOUT.hasTag(l.getUuid()));
        Assert.assertFalse(LoadBalancerSystemTags.HEALTH_INTERVAL.hasTag(l.getUuid()));
        Assert.assertFalse(LoadBalancerSystemTags.HEALTHY_THRESHOLD.hasTag(l.getUuid()));
        Assert.assertFalse(LoadBalancerSystemTags.UNHEALTHY_THRESHOLD.hasTag(l.getUuid()));
        Assert.assertFalse(LoadBalancerSystemTags.HEALTH_TIMEOUT.hasTag(l.getUuid()));
        Assert.assertFalse(LoadBalancerSystemTags.MAX_CONNECTION.hasTag(l.getUuid()));
        Assert.assertFalse(LoadBalancerSystemTags.BALANCER_ALGORITHM.hasTag(l.getUuid()));
        Assert.assertFalse(LoadBalancerSystemTags.HEALTH_TARGET.hasTag(l.getUuid()));

        Long CONNECTION_IDLE_TIMEOUT = Long.valueOf(100);
        final String ns1 = LoadBalancerSystemTags.CONNECTION_IDLE_TIMEOUT.instantiateTag(
                map(e(LoadBalancerSystemTags.CONNECTION_IDLE_TIMEOUT_TOKEN, CONNECTION_IDLE_TIMEOUT))
        );
        Long HEALTH_INTERVAL = Long.valueOf(100);
        final String ns2 = LoadBalancerSystemTags.HEALTH_INTERVAL.instantiateTag(
                map(e(LoadBalancerSystemTags.HEALTH_INTERVAL_TOKEN, HEALTH_INTERVAL))
        );
        Long HEALTHY_THRESHOLD = Long.valueOf(100);
        final String ns3 = LoadBalancerSystemTags.HEALTHY_THRESHOLD.instantiateTag(
                map(e(LoadBalancerSystemTags.HEALTHY_THRESHOLD_TOKEN, HEALTHY_THRESHOLD))
        );
        Long UNHEALTHY_THRESHOLD = Long.valueOf(10);
        final String ns4 = LoadBalancerSystemTags.UNHEALTHY_THRESHOLD.instantiateTag(
                map(e(LoadBalancerSystemTags.UNHEALTHY_THRESHOLD_TOKEN, UNHEALTHY_THRESHOLD))
        );
        Long HEALTH_TIMEOUT = Long.valueOf(8);
        final String ns5 = LoadBalancerSystemTags.HEALTH_TIMEOUT.instantiateTag(
                map(e(LoadBalancerSystemTags.HEALTH_TIMEOUT_TOKEN, HEALTH_TIMEOUT))
        );
        Long MAX_CONNECTION = Long.valueOf(10000);
        final String ns6 = LoadBalancerSystemTags.MAX_CONNECTION.instantiateTag(
                map(e(LoadBalancerSystemTags.MAX_CONNECTION_TOKEN, MAX_CONNECTION))
        );
        String BALANCER_ALGORITHM = "source";
        final String ns7 = LoadBalancerSystemTags.BALANCER_ALGORITHM.instantiateTag(
                map(e(LoadBalancerSystemTags.BALANCER_ALGORITHM_TOKEN, BALANCER_ALGORITHM))
        );
        String HEALTH_TARGET = "tcp:5000";
        final String ns8 = LoadBalancerSystemTags.HEALTH_TARGET.instantiateTag(
                map(e(LoadBalancerSystemTags.HEALTH_TARGET_TOKEN, HEALTH_TARGET))
        );

        vconfig.refreshLbCmds.clear();
        final LoadBalancerListenerInventory l2 = api.createLoadBalancerListener(l, list(ns1, ns2, ns3, ns4, ns5, ns6, ns7, ns8), null);
        api.addVmNicToLoadBalancerListener(l2.getUuid(), nic.getUuid());

        cmd = vconfig.refreshLbCmds.get(0);
        to = CollectionUtils.find(cmd.getLbs(), new Function<LbTO, LbTO>() {
            @Override
            public LbTO call(LbTO arg) {
                return arg.getListenerUuid().equals(l2.getUuid()) ? arg : null;
            }
        });
        String ss1 = LoadBalancerSystemTags.CONNECTION_IDLE_TIMEOUT.getTokenByResourceUuid(l.getUuid(), LoadBalancerSystemTags.CONNECTION_IDLE_TIMEOUT_TOKEN);
        val = Long.valueOf(ss1);
        Assert.assertEquals(CONNECTION_IDLE_TIMEOUT, val);
        tval = CollectionUtils.find(to.getParameters(), new Function<String, String>() {
            @Override
            public String call(String arg) {
                return arg.equals(ns1) ? arg : null;
            }
        });
        Assert.assertNotNull(JSONObjectUtil.toJsonString(to), tval);

        String ss2 = LoadBalancerSystemTags.HEALTH_INTERVAL.getTokenByResourceUuid(l.getUuid(), LoadBalancerSystemTags.HEALTH_INTERVAL_TOKEN);
        val = Long.valueOf(ss2);
        Assert.assertEquals(HEALTH_INTERVAL, val);
        tval = CollectionUtils.find(to.getParameters(), new Function<String, String>() {
            @Override
            public String call(String arg) {
                return arg.equals(ns2) ? arg : null;
            }
        });
        Assert.assertNotNull(tval);

        String ss3 = LoadBalancerSystemTags.HEALTHY_THRESHOLD.getTokenByResourceUuid(l.getUuid(), LoadBalancerSystemTags.HEALTHY_THRESHOLD_TOKEN);
        val = Long.valueOf(ss3);
        Assert.assertEquals(HEALTHY_THRESHOLD, val);
        tval = CollectionUtils.find(to.getParameters(), new Function<String, String>() {
            @Override
            public String call(String arg) {
                return arg.equals(ns3) ? arg : null;
            }
        });
        Assert.assertNotNull(tval);

        String ss4 = LoadBalancerSystemTags.UNHEALTHY_THRESHOLD.getTokenByResourceUuid(l.getUuid(), LoadBalancerSystemTags.UNHEALTHY_THRESHOLD_TOKEN);
        val = Long.valueOf(ss4);
        Assert.assertEquals(UNHEALTHY_THRESHOLD, val);
        tval = CollectionUtils.find(to.getParameters(), new Function<String, String>() {
            @Override
            public String call(String arg) {
                return arg.equals(ns4) ? arg : null;
            }
        });
        Assert.assertNotNull(tval);

        String ss5 = LoadBalancerSystemTags.HEALTH_TIMEOUT.getTokenByResourceUuid(l.getUuid(), LoadBalancerSystemTags.HEALTH_TIMEOUT_TOKEN);
        val = Long.valueOf(ss5);
        Assert.assertEquals(HEALTH_TIMEOUT, val);
        tval = CollectionUtils.find(to.getParameters(), new Function<String, String>() {
            @Override
            public String call(String arg) {
                return arg.equals(ns5) ? arg : null;
            }
        });
        Assert.assertNotNull(tval);

        String ss6 = LoadBalancerSystemTags.MAX_CONNECTION.getTokenByResourceUuid(l.getUuid(), LoadBalancerSystemTags.MAX_CONNECTION_TOKEN);
        val = Long.valueOf(ss6);
        Assert.assertEquals(MAX_CONNECTION, val);
        tval = CollectionUtils.find(to.getParameters(), new Function<String, String>() {
            @Override
            public String call(String arg) {
                return arg.equals(ns6) ? arg : null;
            }
        });
        Assert.assertNotNull(tval);

        String ss7 = LoadBalancerSystemTags.BALANCER_ALGORITHM.getTokenByResourceUuid(l.getUuid(), LoadBalancerSystemTags.BALANCER_ALGORITHM_TOKEN);
        Assert.assertEquals(BALANCER_ALGORITHM, ss7);
        tval = CollectionUtils.find(to.getParameters(), new Function<String, String>() {
            @Override
            public String call(String arg) {
                return arg.equals(ns7) ? arg : null;
            }
        });
        Assert.assertNotNull(tval);

        String ss8 = LoadBalancerSystemTags.HEALTH_TARGET.getTokenByResourceUuid(l.getUuid(), LoadBalancerSystemTags.HEALTH_TARGET_TOKEN);
        Assert.assertEquals(HEALTH_TARGET, ss8);
        tval = CollectionUtils.find(to.getParameters(), new Function<String, String>() {
            @Override
            public String call(String arg) {
                return arg.equals(ns8) ? arg : null;
            }
        });
        Assert.assertNotNull(tval);
    }
}
