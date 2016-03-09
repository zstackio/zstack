package org.zstack.test.compute.zone;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.search.APISearchMessage;
import org.zstack.header.search.SearchOp;
import org.zstack.header.zone.APIGetZoneMsg;
import org.zstack.header.zone.APISearchZoneMsg;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.header.zone.ZoneVO;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class TestSearchZone {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/vm/TestSearchZone.xml");
        deployer.addSpringConfig("SearchManager.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        TimeUnit.SECONDS.sleep(2);
        APISearchZoneMsg msg = new APISearchZoneMsg();
        String content = api.search(msg);
        ArrayList<ZoneInventory> lst = JSONObjectUtil.toCollection(content, ArrayList.class, ZoneInventory.class);
        Assert.assertEquals(5, lst.size());
        
        APISearchZoneMsg msg1 = new APISearchZoneMsg();
        APISearchMessage.NOVTriple t1 = new APISearchMessage.NOVTriple();
        t1.setName("name");
        t1.setOp(SearchOp.AND_EQ.toString());
        t1.setVal("Zone1");
        msg1.getNameOpValueTriples().add(t1);
        content = api.search(msg1);
        ZoneInventory inv = (ZoneInventory) JSONObjectUtil.toCollection(content, ArrayList.class, ZoneInventory.class).get(0);
        ZoneVO vo = dbf.findByUuid(inv.getUuid(), ZoneVO.class);
        Assert.assertEquals(inv.getName(), vo.getName());
        
        APIGetZoneMsg gmsg = new APIGetZoneMsg();
        gmsg.setUuid(inv.getUuid());
        String res = api.getInventory(gmsg);
        ZoneInventory zinv = JSONObjectUtil.toObject(res, ZoneInventory.class);
        Assert.assertEquals(inv.getName(), zinv.getName());
    }
}
