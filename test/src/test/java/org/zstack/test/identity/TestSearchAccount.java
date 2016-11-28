package org.zstack.test.identity;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.APIGetAccountMsg;
import org.zstack.header.identity.APISearchAccountMsg;
import org.zstack.header.identity.AccountConstant;
import org.zstack.header.identity.AccountInventory;
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

public class TestSearchAccount {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/identity/TestSearchAccount.xml");
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
        APISearchAccountMsg msg = new APISearchAccountMsg();
        NOVTriple t = new NOVTriple();
        t.setName("name");
        t.setOp(SearchOp.AND_NOT_EQ.toString());
        t.setVal(AccountConstant.INITIAL_SYSTEM_ADMIN_NAME);
        msg.getNameOpValueTriples().add(t);
        String content = api.search(msg);
        List<AccountInventory> invs = JSONObjectUtil.toCollection(content, ArrayList.class, AccountInventory.class);
        Assert.assertEquals(3, invs.size());
        AccountInventory inv0 = invs.get(0);
        APIGetAccountMsg gmsg = new APIGetAccountMsg();
        gmsg.setUuid(inv0.getUuid());
        String res = api.getInventory(gmsg);
        AccountInventory ainv = JSONObjectUtil.toObject(res, AccountInventory.class);
        Assert.assertEquals(inv0.getName(), ainv.getName());
    }

}
