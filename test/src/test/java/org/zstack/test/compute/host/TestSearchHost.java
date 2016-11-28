package org.zstack.test.compute.host;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.host.APIGetHostMsg;
import org.zstack.header.host.APISearchHostMsg;
import org.zstack.header.host.HostInventory;
import org.zstack.header.search.APISearchMessage.NOLTriple;
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

public class TestSearchHost {
    CLogger logger = Utils.getLogger(TestSearchHost.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/host/TestSearchHost.xml");
        deployer.addSpringConfig("SearchManager.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws InterruptedException, ApiSenderException {
        TimeUnit.SECONDS.sleep(2);
        APISearchHostMsg msg = new APISearchHostMsg();
        NOLTriple t = new NOLTriple();
        t.setName("hostTags");
        t.setOp(SearchOp.AND_IN.toString());
        t.getVals().add("large");
        t.getVals().add("web");
        t.getVals().add("test");
        msg.getNameOpListTriples().add(t);
        String content = api.search(msg);
        List<HostInventory> invs = JSONObjectUtil.toCollection(content, ArrayList.class, HostInventory.class);
        Assert.assertEquals(3, invs.size());

        HostInventory inv0 = invs.get(0);
        APIGetHostMsg gmsg = new APIGetHostMsg();
        gmsg.setUuid(inv0.getUuid());
        String res = api.getInventory(gmsg);
        HostInventory hinv = JSONObjectUtil.toObject(res, HostInventory.class);
        Assert.assertEquals(inv0.getName(), hinv.getName());

        APISearchHostMsg msg1 = new APISearchHostMsg();
        content = api.search(msg1);
        invs = JSONObjectUtil.toCollection(content, ArrayList.class, HostInventory.class);
        for (HostInventory inv : invs) {
            api.maintainHost(inv.getUuid());
            api.deleteHost(inv.getUuid());
        }

        TimeUnit.SECONDS.sleep(2);
        content = api.search(msg1);
        invs = JSONObjectUtil.toCollection(content, ArrayList.class, HostInventory.class);
        Assert.assertEquals(0, invs.size());
    }
}
