package org.zstack.test.virtualrouter;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.search.APISearchMessage.NOVTriple;
import org.zstack.header.search.SearchOp;
import org.zstack.network.service.virtualrouter.APIGetVirtualRouterOfferingMsg;
import org.zstack.network.service.virtualrouter.APISearchVirtualRouterOffingMsg;
import org.zstack.network.service.virtualrouter.VirtualRouterConstant;
import org.zstack.network.service.virtualrouter.VirtualRouterOfferingInventory;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TestSearchVirtualRouterOffering {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/virtualRouter/virtualRouterOffering.xml", con);
        deployer.addSpringConfig("VirtualRouter.xml");
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("SearchManager.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        TimeUnit.SECONDS.sleep(1);
        APISearchVirtualRouterOffingMsg msg = new APISearchVirtualRouterOffingMsg();
        NOVTriple t = new NOVTriple();
        t.setName("type");
        t.setOp(SearchOp.AND_EQ.toString());
        t.setVal(VirtualRouterConstant.VIRTUAL_ROUTER_OFFERING_TYPE);
        msg.getNameOpValueTriples().add(t);

        String res = api.search(msg);
        List<VirtualRouterOfferingInventory> invs = JSONObjectUtil.toCollection(res, ArrayList.class, VirtualRouterOfferingInventory.class);
        Assert.assertEquals(1, invs.size());

        VirtualRouterOfferingInventory inv0 = invs.get(0);
        APIGetVirtualRouterOfferingMsg gmsg = new APIGetVirtualRouterOfferingMsg();
        gmsg.setUuid(inv0.getUuid());
        res = api.getInventory(gmsg);
        VirtualRouterOfferingInventory vinv = JSONObjectUtil.toObject(res, VirtualRouterOfferingInventory.class);
        Assert.assertEquals(inv0.getName(), vinv.getName());

        api.deleteInstanceOffering(vinv.getUuid());
        TimeUnit.SECONDS.sleep(1);
        res = api.getInventory(gmsg);
        Assert.assertNull(res);
    }
}
