package org.zstack.test.vip;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.network.l3.APIGetIpAddressCapacityReply;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.network.service.eip.EipInventory;
import org.zstack.network.service.vip.VipInventory;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.simulator.virtualrouter.VirtualRouterSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;

import java.util.Arrays;

/**
 * @author frank
 * @condition 1. create a vip1
 * 2. delete vip1
 * 3. delete the eip
 * 4. delete vip the eip previously used
 * @test confirm ip of vips are return to capacity
 */
public class TestVipDeletion {
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
        deployer = new Deployer("deployerXml/vip/TestVipDeletion.xml", con);
        deployer.addSpringConfig("VirtualRouter.xml");
        deployer.addSpringConfig("VirtualRouterSimulator.xml");
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("vip.xml");
        deployer.addSpringConfig("eip.xml");
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
        L3NetworkInventory pubL3 = deployer.l3Networks.get("PublicNetwork");
        APIGetIpAddressCapacityReply reply = api.getIpAddressCapacity(null, Arrays.asList(pubL3.getUuid()), null);
        long originCapacity = reply.getAvailableCapacity();
        VipInventory vip = api.acquireIp(pubL3.getUuid());
        reply = api.getIpAddressCapacity(null, Arrays.asList(pubL3.getUuid()), null);
        long afterCapacity = reply.getAvailableCapacity();
        Assert.assertEquals(originCapacity - 1, afterCapacity);
        api.releaseIp(vip.getUuid());
        reply = api.getIpAddressCapacity(null, Arrays.asList(pubL3.getUuid()), null);
        afterCapacity = reply.getAvailableCapacity();
        Assert.assertEquals(originCapacity, afterCapacity);

        EipInventory eip = deployer.eips.get("eip");
        api.releaseIp(eip.getVipUuid());
        reply = api.getIpAddressCapacity(null, Arrays.asList(pubL3.getUuid()), null);
        afterCapacity = reply.getAvailableCapacity();
        Assert.assertEquals(originCapacity + 1, afterCapacity);
    }
}
