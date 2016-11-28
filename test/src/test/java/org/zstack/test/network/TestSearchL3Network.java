package org.zstack.test.network;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.network.l3.APIGetL3NetworkMsg;
import org.zstack.header.network.l3.APISearchL3NetworkMsg;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.search.APISearchMessage.NOVTriple;
import org.zstack.header.search.SearchOp;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TestSearchL3Network {
    CLogger logger = Utils.getLogger(TestSearchL3Network.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/network/TestSearchL3Network.xml");
        deployer.addSpringConfig("SearchManager.xml");
        deployer.addSpringConfig("VirtualRouter.xml");
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        TimeUnit.SECONDS.sleep(1);
        APISearchL3NetworkMsg msg = new APISearchL3NetworkMsg();
        NOVTriple t = new NOVTriple();
        t.setName("ipRanges.gateway");
        t.setOp(SearchOp.AND_NOT_EQ.toString());
        t.setVal("10.0.0.1");
        msg.getNameOpValueTriples().add(t);

        String content = api.search(msg);
        List<L3NetworkInventory> invs = JSONObjectUtil.toCollection(content, ArrayList.class, L3NetworkInventory.class);
        Assert.assertEquals(2, invs.size());

        msg = new APISearchL3NetworkMsg();
        t.setName("dns");
        t.setOp(SearchOp.AND_EQ.toString());
        t.setVal("8.8.8.8");
        msg.getNameOpValueTriples().add(t);
        content = api.search(msg);
        invs = JSONObjectUtil.toCollection(content, ArrayList.class, L3NetworkInventory.class);
        Assert.assertEquals(1, invs.size());
        Assert.assertEquals("TestL3Network3", invs.get(0).getName());

        msg = new APISearchL3NetworkMsg();
        t.setName("networkServices.networkServiceTypes");
        t.setOp(SearchOp.AND_EQ.toString());
        t.setVal("DHCP");
        msg.getNameOpValueTriples().add(t);
        content = api.search(msg);
        invs = JSONObjectUtil.toCollection(content, ArrayList.class, L3NetworkInventory.class);
        Assert.assertEquals(1, invs.size());
        Assert.assertEquals("TestL3Network1", invs.get(0).getName());

        L3NetworkInventory linv = invs.get(0);
        APIGetL3NetworkMsg gmsg = new APIGetL3NetworkMsg();
        gmsg.setUuid(linv.getUuid());
        String res = api.getInventory(gmsg);
        L3NetworkInventory l3inv = JSONObjectUtil.toObject(res, L3NetworkInventory.class);
        Assert.assertEquals(linv.getName(), l3inv.getName());
    }

}
