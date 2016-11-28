package org.zstack.test.virtualrouter;

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
import org.zstack.header.network.service.NetworkServiceL3NetworkRefInventory;
import org.zstack.header.network.service.NetworkServiceProviderVO;
import org.zstack.header.network.service.NetworkServiceProviderVO_;
import org.zstack.network.service.eip.EipConstant;
import org.zstack.network.service.lb.LoadBalancerConstants;
import org.zstack.network.service.virtualrouter.VirtualRouterConstant;
import org.zstack.simulator.appliancevm.ApplianceVmSimulatorConfig;
import org.zstack.simulator.virtualrouter.VirtualRouterSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by frank on 8/31/2015.
 */
public class TestVirtualRouterAttachNetworkService {
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
        deployer = new Deployer("deployerXml/virtualRouter/TestVirtualRouterAttachNetworkService.xml", con);
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
        SimpleQuery<NetworkServiceProviderVO> q = dbf.createQuery(NetworkServiceProviderVO.class);
        q.add(NetworkServiceProviderVO_.type, Op.EQ, VirtualRouterConstant.VIRTUAL_ROUTER_PROVIDER_TYPE);
        NetworkServiceProviderVO vrp = q.find();

        L3NetworkInventory l3 = deployer.l3Networks.get("GuestNetwork");
        l3 = api.attachNetworkServiceToL3Network(l3.getUuid(), vrp.getUuid(), list(EipConstant.EIP_NETWORK_SERVICE_TYPE, LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING));
        boolean lb = false;
        boolean eip = false;
        for (NetworkServiceL3NetworkRefInventory ref : l3.getNetworkServices()) {
            if (ref.getNetworkServiceType().equals(EipConstant.EIP_NETWORK_SERVICE_TYPE)) {
                eip = true;
            }
            if (ref.getNetworkServiceType().equals(LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING)) {
                lb = true;
            }
        }

        Assert.assertTrue(lb);
        Assert.assertTrue(eip);

        boolean s = false;
        try {
            api.attachNetworkServiceToL3Network(l3.getUuid(), vrp.getUuid(), list(EipConstant.EIP_NETWORK_SERVICE_TYPE, LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING));
        } catch (ApiSenderException e) {
            s = true;
        }
        Assert.assertTrue(s);
    }
}
