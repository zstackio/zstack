package org.zstack.test.configuration;

import junit.framework.Assert;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.configuration.APISearchInstanceOfferingMsg;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.search.APISearchMessage.NOVTriple;
import org.zstack.header.search.SearchOp;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TestSearchInstanceOffering {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/configuration/TestSearchInstanceOffering.xml");
        deployer.addSpringConfig("SearchManager.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException, JSONException {
        TimeUnit.SECONDS.sleep(5);
        APISearchInstanceOfferingMsg msg = new APISearchInstanceOfferingMsg();
        NOVTriple t = new NOVTriple();
        t.setName("cpuNum");
        t.setOp(SearchOp.AND_GTE.toString());
        t.setVal("3");
        msg.getNameOpValueTriples().add(t);
        String content = api.search(msg);
        List<InstanceOfferingInventory> invs = JSONObjectUtil.toCollection(content, ArrayList.class, InstanceOfferingInventory.class);
        Assert.assertEquals(3, invs.size());
        Assert.assertTrue(invs.get(0).getCpuNum() >= 3);

        msg = new APISearchInstanceOfferingMsg();
        t.setName("memorySize");
        t.setOp(SearchOp.AND_GTE.toString());
        t.setVal(String.valueOf(SizeUnit.GIGABYTE.toByte(3)));
        msg.getNameOpValueTriples().add(t);
        NOVTriple t1 = new NOVTriple();
        t1.setName("cpuSpeed");
        t1.setOp(SearchOp.OR_LT.toString());
        t1.setVal("3000");
        msg.getNameOpValueTriples().add(t1);
        content = api.search(msg);
        invs = JSONObjectUtil.toCollection(content, ArrayList.class, InstanceOfferingInventory.class);
        Assert.assertEquals(5, invs.size());
    }

}
