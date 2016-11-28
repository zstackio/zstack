package org.zstack.test.lb;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.tag.SystemTagInventory;
import org.zstack.network.service.lb.LoadBalancerInventory;
import org.zstack.network.service.lb.LoadBalancerListenerVO;
import org.zstack.network.service.lb.LoadBalancerSystemTags;
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

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 * @author frank
 *         <p>
 *         1. create a lb
 *         2. delete a lb system tag
 *         <p>
 *         confirm unable to delete
 *         <p>
 *         3. update the system tag
 *         4. refresh the lb
 *         <p>
 *         confirm lb refreshed successfully
 *         <p>
 *         5. update the sysetem tag to an invalid value
 *         <p>
 *         confirm unable to update
 */
public class TestVirtualRouterLb16 {
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
        SystemTagInventory tag = LoadBalancerSystemTags.UNHEALTHY_THRESHOLD.getTagInventory(listenerVO.getUuid());

        boolean s = false;
        try {
            api.deleteTag(tag.getUuid());
        } catch (ApiSenderException e) {
            s = true;
        }
        Assert.assertTrue(s);

        int newval = 66;
        api.updateSystemTag(tag.getUuid(), LoadBalancerSystemTags.UNHEALTHY_THRESHOLD.instantiateTag(map(e(LoadBalancerSystemTags.UNHEALTHY_THRESHOLD_TOKEN, newval))), null);
        vconfig.refreshLbCmds.clear();
        api.refreshLoadBalancer(lbvo.getUuid());
        RefreshLbCmd cmd = vconfig.refreshLbCmds.get(0);
        Assert.assertFalse(cmd.getLbs().isEmpty());
        LbTO to = cmd.getLbs().get(0);
        String unhealthThreshold = CollectionUtils.find(to.getParameters(), new Function<String, String>() {
            @Override
            public String call(String arg) {
                return arg.contains(LoadBalancerSystemTags.UNHEALTHY_THRESHOLD_TOKEN) ? arg : null;
            }
        });

        String val = LoadBalancerSystemTags.UNHEALTHY_THRESHOLD.getTokenByTag(unhealthThreshold, LoadBalancerSystemTags.UNHEALTHY_THRESHOLD_TOKEN);
        Assert.assertEquals(Integer.valueOf(newval), Integer.valueOf(val));

        s = false;
        try {
            api.updateSystemTag(tag.getUuid(), LoadBalancerSystemTags.UNHEALTHY_THRESHOLD.instantiateTag(map(e(LoadBalancerSystemTags.UNHEALTHY_THRESHOLD_TOKEN, "invalid"))), null);
        } catch (ApiSenderException e) {
            s = true;
        }
        Assert.assertTrue(s);
    }
}
