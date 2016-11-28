package org.zstack.test.identity;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.*;
import org.zstack.header.search.APISearchMessage.NOVTriple;
import org.zstack.header.search.SearchOp;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class TestSearchUserGroup {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/identity/TestSearchUserGroup.xml");
        deployer.addSpringConfig("SearchManager.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws InterruptedException, ApiSenderException {
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
        APISearchUserGroupMsg msg = new APISearchUserGroupMsg();
        msg.setSession(session);
        t.setName("policies.name");
        t.setOp(SearchOp.AND_EQ.toString());
        t.setVal("policy1");
        msg.getNameOpValueTriples().add(t);
        content = api.search(msg);
        UserGroupInventory uginv = (UserGroupInventory) JSONObjectUtil.toCollection(content, ArrayList.class, UserGroupInventory.class).get(0);
        Assert.assertEquals("group1", uginv.getName());

        APIGetUserGroupMsg gmsg = new APIGetUserGroupMsg();
        gmsg.setUuid(uginv.getUuid());
        String res = api.getInventory(gmsg);
        UserGroupInventory uuginv = JSONObjectUtil.toObject(res, UserGroupInventory.class);
        Assert.assertEquals(uginv.getName(), uuginv.getName());
    }
}
