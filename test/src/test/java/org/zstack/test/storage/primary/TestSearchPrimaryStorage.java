package org.zstack.test.storage.primary;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.search.APISearchMessage.NOVTriple;
import org.zstack.header.search.SearchOp;
import org.zstack.header.storage.primary.APIGetPrimaryStorageMsg;
import org.zstack.header.storage.primary.APISearchPrimaryStorageMsg;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TestSearchPrimaryStorage {
    CLogger logger = Utils.getLogger(TestSearchPrimaryStorage.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/primaryStorage/TestSearchPrimaryStorage.xml");
        deployer.addSpringConfig("SearchManager.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        TimeUnit.SECONDS.sleep(1);
        APISearchPrimaryStorageMsg msg = new APISearchPrimaryStorageMsg();
        NOVTriple t = new NOVTriple();
        t.setName("totalCapacity");
        t.setOp(SearchOp.AND_GT.toString());
        t.setVal(String.valueOf(SizeUnit.TERABYTE.toByte(1)));
        msg.getNameOpValueTriples().add(t);

        String content = api.search(msg);
        List<PrimaryStorageInventory> invs = JSONObjectUtil.toCollection(content, ArrayList.class, PrimaryStorageInventory.class);
        Assert.assertEquals(2, invs.size());

        PrimaryStorageInventory inv = null;
        for (PrimaryStorageInventory i : invs) {
            if (i.getName().equals("TestPrimaryStorage2")) {
                inv = i;
                break;
            }
        }

        Assert.assertFalse(inv.getAttachedClusterUuids().isEmpty());
        api.detachPrimaryStorage(inv.getUuid(), inv.getAttachedClusterUuids().iterator().next());
        TimeUnit.SECONDS.sleep(1);

        msg = new APISearchPrimaryStorageMsg();
        t = new NOVTriple();
        t.setName("name");
        t.setOp(SearchOp.AND_GT.toString());
        t.setVal("TestPrimaryStorage2");
        msg.getNameOpValueTriples().add(t);
        content = api.search(msg);
        invs = JSONObjectUtil.toCollection(content, ArrayList.class, PrimaryStorageInventory.class);
        inv = invs.get(0);
        Assert.assertTrue(inv.getAttachedClusterUuids().isEmpty());

        APIGetPrimaryStorageMsg gmsg = new APIGetPrimaryStorageMsg();
        gmsg.setUuid(inv.getUuid());
        String res = api.getInventory(gmsg);
        PrimaryStorageInventory pinv = JSONObjectUtil.toObject(res, PrimaryStorageInventory.class);
        Assert.assertEquals(inv.getName(), pinv.getName());
    }
}
