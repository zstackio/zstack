package org.zstack.test.image;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.image.APIGetImageMsg;
import org.zstack.header.image.APISearchImageMsg;
import org.zstack.header.image.ImageInventory;
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

public class TestSearchImage {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/image/TestSearchImage.xml");
        deployer.addSpringConfig("SearchManager.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws InterruptedException, ApiSenderException {
        TimeUnit.SECONDS.sleep(1);
        APISearchImageMsg msg = new APISearchImageMsg();
        NOVTriple t = new NOVTriple();
        t.setName("name");
        t.setOp(SearchOp.AND_EQ.toString());
        t.setVal("TestImage");
        msg.getNameOpValueTriples().add(t);

        t = new NOVTriple();
        t.setName("url");
        t.setOp(SearchOp.AND_EQ.toString());
        t.setVal("http://zstack.org/download/test.qcow2");
        msg.getNameOpValueTriples().add(t);

        String content = api.search(msg);
        List<ImageInventory> invs = JSONObjectUtil.toCollection(content, ArrayList.class, ImageInventory.class);
        Assert.assertEquals(1, invs.size());

        ImageInventory inv0 = invs.get(0);
        APIGetImageMsg gmsg = new APIGetImageMsg();
        gmsg.setUuid(inv0.getUuid());
        String res = api.getInventory(gmsg);
        ImageInventory iinv = JSONObjectUtil.toObject(res, ImageInventory.class);
        Assert.assertEquals(inv0.getName(), iinv.getName());
    }
}
