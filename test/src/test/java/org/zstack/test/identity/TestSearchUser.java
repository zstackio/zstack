package org.zstack.test.identity;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.*;
import org.zstack.header.search.APISearchMessage.NOLTriple;
import org.zstack.header.search.APISearchMessage.NOVTriple;
import org.zstack.header.search.SearchOp;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TestSearchUser {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/identity/TestSearchUser.xml");
        deployer.addSpringConfig("SearchManager.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        TimeUnit.SECONDS.sleep(5);
        APISearchAccountMsg amsg = new APISearchAccountMsg();
        NOVTriple t = new NOVTriple();
        t.setName("name");
        t.setOp(SearchOp.AND_EQ.toString());
        t.setVal("account1");
        amsg.getNameOpValueTriples().add(t);
        String content = api.search(amsg);
        AccountInventory ainv = (AccountInventory) JSONObjectUtil.toCollection(content, ArrayList.class, AccountInventory.class).get(0);

        SessionInventory session = api.loginByAccount(ainv.getName(), "password");
        APISearchUserMsg msg = new APISearchUserMsg();
        msg.setSession(session);
        t = new NOVTriple();
        t.setName("groups.name");
        t.setOp(SearchOp.AND_EQ.toString());
        t.setVal("group1");
        msg.getNameOpValueTriples().add(t);
        content = api.search(msg);
        UserInventory uinv = (UserInventory) JSONObjectUtil.toCollection(content, ArrayList.class, UserInventory.class).get(0);
        Assert.assertEquals("user1", uinv.getName());

        APIGetUserMsg gmsg = new APIGetUserMsg();
        gmsg.setUuid(uinv.getUuid());
        String res = api.getInventory(gmsg);
        UserInventory uuinv = JSONObjectUtil.toObject(res, UserInventory.class);
        Assert.assertEquals(uinv.getName(), uuinv.getName());

        msg = new APISearchUserMsg();
        msg.setSession(session);
        NOLTriple lt = new NOLTriple();
        lt.setName("policies.name");
        lt.setOp(SearchOp.AND_NOT_IN.toString());
        List<String> p = new ArrayList<String>();
        p.add("policy1");
        lt.setVals(p);
        t.setName("name");
        t.setOp(SearchOp.AND_NOT_EQ.toString());
        t.setVal("account1");
        msg.getNameOpListTriples().add(lt);
        msg.getNameOpValueTriples().add(t);
        content = api.search(msg);
        List<UserInventory> uinvs = JSONObjectUtil.toCollection(content, ArrayList.class, UserInventory.class);
        Assert.assertEquals(2, uinvs.size());
    }

}
