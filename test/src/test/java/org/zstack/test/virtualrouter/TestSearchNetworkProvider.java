package org.zstack.test.virtualrouter;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.network.service.APIGetNetworkServiceProviderMsg;
import org.zstack.header.network.service.APISearchNetworkServiceProviderMsg;
import org.zstack.header.network.service.NetworkServiceProviderInventory;
import org.zstack.header.search.APISearchMessage.NOVTriple;
import org.zstack.header.search.SearchOp;
import org.zstack.network.service.virtualrouter.VirtualRouterConstant;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.simulator.virtualrouter.VirtualRouterSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;

public class TestSearchNetworkProvider {
    CLogger logger = Utils.getLogger(TestSearchNetworkProvider.class);
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
        deployer = new Deployer("deployerXml/virtualRouter/TestSearchNetworkProvider.xml", con);
        deployer.addSpringConfig("VirtualRouter.xml");
        deployer.addSpringConfig("VirtualRouterSimulator.xml");
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("SearchManager.xml");
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
        APISearchNetworkServiceProviderMsg msg = new APISearchNetworkServiceProviderMsg();
        NOVTriple t = new NOVTriple();
        t.setName("type");
        t.setOp(SearchOp.AND_EQ.toString());
        t.setVal(VirtualRouterConstant.VIRTUAL_ROUTER_PROVIDER_TYPE);
        msg.getNameOpValueTriples().add(t);

        String res = api.search(msg);
        List<NetworkServiceProviderInventory> invs = JSONObjectUtil.toCollection(res, ArrayList.class, NetworkServiceProviderInventory.class);
        Assert.assertEquals(1, invs.size());

        NetworkServiceProviderInventory inv0 = invs.get(0);
        APIGetNetworkServiceProviderMsg gmsg = new APIGetNetworkServiceProviderMsg();
        gmsg.setUuid(inv0.getUuid());
        res = api.getInventory(gmsg);
        NetworkServiceProviderInventory ninv = JSONObjectUtil.toObject(res, NetworkServiceProviderInventory.class);
        Assert.assertEquals(inv0.getName(), ninv.getName());
    }
}
