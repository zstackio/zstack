package org.zstack.test.virtualrouter;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.service.NetworkServiceProviderVO;
import org.zstack.header.network.service.NetworkServiceProviderVO_;
import org.zstack.header.network.service.NetworkServiceType;
import org.zstack.network.service.eip.EipConstant;
import org.zstack.network.service.virtualrouter.VirtualRouterConstant;
import org.zstack.simulator.appliancevm.ApplianceVmSimulatorConfig;
import org.zstack.simulator.virtualrouter.VirtualRouterSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;

import java.util.ArrayList;
import java.util.List;

/**
 * @author frank
 * @condition 1. select port forwarding service
 * 2. dont select source nat service
 * 3. repeat above to eip
 * @test confirm  without snat, port forwarding/eip fail to attach
 */
public class TestVirtualRouterServiceAttaching {
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
        deployer = new Deployer("deployerXml/virtualRouter/TestVirtualRouterServiceAttaching.xml", con);
        deployer.addSpringConfig("VirtualRouter.xml");
        deployer.addSpringConfig("VirtualRouterSimulator.xml");
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("PortForwarding.xml");
        deployer.addSpringConfig("vip.xml");
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
        L3NetworkInventory guestL3 = deployer.l3Networks.get("GuestNetwork");
        SimpleQuery<NetworkServiceProviderVO> q = dbf.createQuery(NetworkServiceProviderVO.class);
        q.add(NetworkServiceProviderVO_.type, Op.EQ, VirtualRouterConstant.VIRTUAL_ROUTER_PROVIDER_TYPE);
        NetworkServiceProviderVO pvo = q.find();
        List<String> lst = new ArrayList<String>();
        lst.add(NetworkServiceType.PortForwarding.toString());

        boolean success1 = false;
        try {
            api.attachNetworkServiceToL3Network(guestL3.getUuid(), pvo.getUuid(), lst);
        } catch (ApiSenderException se) {
            success1 = true;
        }

        Assert.assertTrue(success1);

        lst = new ArrayList<String>();
        lst.add(EipConstant.EIP_NETWORK_SERVICE_TYPE);

        boolean success2 = false;
        try {
            api.attachNetworkServiceToL3Network(guestL3.getUuid(), pvo.getUuid(), lst);
        } catch (ApiSenderException se) {
            success2 = true;
        }

        Assert.assertTrue(success2);

        lst = new ArrayList<String>();
        lst.add(EipConstant.EIP_NETWORK_SERVICE_TYPE);
        lst.add(NetworkServiceType.PortForwarding.toString());
        lst.add(NetworkServiceType.SNAT.toString());
        api.attachNetworkServiceToL3Network(guestL3.getUuid(), pvo.getUuid(), lst);
    }
}
