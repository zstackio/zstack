package org.zstack.test.compute.zone;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.query.QueryCondition;
import org.zstack.header.query.QueryOp;
import org.zstack.header.zone.APIQueryZoneMsg;
import org.zstack.header.zone.APIQueryZoneReply;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.header.zone.ZoneVO;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;

import java.util.ArrayList;
import java.util.List;

public class TestQueryZone {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        // initialize properties
        Platform.getUuid();
        CoreGlobalProperty.CHECK_BOX_TYPE_IN_INVENTORY = true;

        deployer = new Deployer("deployerXml/zone/TestQueryZone.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        APIQueryZoneMsg msg = new APIQueryZoneMsg();
        QueryCondition cond1 = new QueryCondition();
        cond1.setName("name");
        cond1.setOp(QueryOp.EQ.toString());
        cond1.setValue("Zone1");
        msg.setReplyWithCount(true);
        msg.getConditions().add(cond1);
        APIQueryZoneReply reply = api.query(msg, APIQueryZoneReply.class);
        Assert.assertEquals(1, reply.getInventories().size());

        msg = new APIQueryZoneMsg();
        QueryCondition cond2 = new QueryCondition();
        cond2.setName("name");
        cond2.setOp(QueryOp.IN.toString());
        cond2.setValues("Zone1", "Zone2", "Zone3");
        msg.getConditions().add(cond2);
        reply = api.query(msg, APIQueryZoneReply.class);
        Assert.assertEquals(3, reply.getInventories().size());

        msg = new APIQueryZoneMsg();
        msg.setSortBy("name");
        msg.setSortDirection("desc");
        msg.setConditions(new ArrayList<QueryCondition>());
        reply = api.query(msg, APIQueryZoneReply.class);
        List<ZoneInventory> invs = reply.getInventories();
        ZoneInventory first = invs.get(0);
        ZoneInventory last = invs.get(invs.size() - 1);
        Assert.assertEquals("Zone5", first.getName());
        Assert.assertEquals("Zone1", last.getName());

        msg = new APIQueryZoneMsg();
        msg.addField("name");
        msg.addQueryCondition("name", QueryOp.EQ, "Zone1");
        reply = api.query(msg, APIQueryZoneReply.class);
        Assert.assertEquals(1, reply.getInventories().size());
        ZoneInventory zone1 = reply.getInventories().get(0);
        Assert.assertEquals("Zone1", zone1.getName());
        Assert.assertNull(zone1.getUuid());
        Assert.assertNull(zone1.getDescription());
        Assert.assertNull(zone1.getCreateDate());
        Assert.assertNull(zone1.getLastOpDate());

        msg = new APIQueryZoneMsg();
        msg.addQueryCondition("name", QueryOp.LIKE, "Zone%");
        reply = api.query(msg, APIQueryZoneReply.class);
        Assert.assertEquals(5, reply.getInventories().size());

        msg = new APIQueryZoneMsg();
        msg.addQueryCondition("name", QueryOp.NOT_LIKE, "%one%");
        reply = api.query(msg, APIQueryZoneReply.class);
        Assert.assertEquals(0, reply.getInventories().size());

        zone1 = deployer.zones.get("Zone1");
        api.createUserTag(zone1.getUuid(), "userTag", ZoneVO.class);
        msg = new APIQueryZoneMsg();
        msg.addQueryCondition("__userTag__", QueryOp.IN, "userTag");
        reply = api.query(msg, APIQueryZoneReply.class);
        Assert.assertEquals(1, reply.getInventories().size());
        ZoneInventory inv = reply.getInventories().get(0);
        Assert.assertEquals(zone1.getUuid(), inv.getUuid());

        msg = new APIQueryZoneMsg();
        msg.addQueryCondition("__userTag__", QueryOp.IN, "userTag");
        msg.addQueryCondition("name", QueryOp.EQ, "1");
        reply = api.query(msg, APIQueryZoneReply.class);
        Assert.assertTrue(reply.getInventories().isEmpty());

        msg = new APIQueryZoneMsg();
        msg.addQueryCondition("__userTag__", QueryOp.EQ, "userTag");
        reply = api.query(msg, APIQueryZoneReply.class);
        Assert.assertEquals(1, reply.getInventories().size());

        msg = new APIQueryZoneMsg();
        msg.addQueryCondition("__userTag__", QueryOp.LIKE, "userTa%");
        reply = api.query(msg, APIQueryZoneReply.class);
        Assert.assertEquals(1, reply.getInventories().size());

        msg = new APIQueryZoneMsg();
        msg.addQueryCondition("__userTag__", QueryOp.NOT_LIKE, "userTa%");
        reply = api.query(msg, APIQueryZoneReply.class);
        Assert.assertEquals(4, reply.getInventories().size());

        msg = new APIQueryZoneMsg();
        msg.addQueryCondition("__userTag__", QueryOp.NOT_EQ, "userTag");
        reply = api.query(msg, APIQueryZoneReply.class);
        Assert.assertEquals(4, reply.getInventories().size());

        msg = new APIQueryZoneMsg();
        msg.addQueryCondition("__userTag__", QueryOp.IS_NULL);
        reply = api.query(msg, APIQueryZoneReply.class);
        Assert.assertEquals(4, reply.getInventories().size());

        msg = new APIQueryZoneMsg();
        msg.addQueryCondition("__userTag__", QueryOp.NOT_NULL);
        reply = api.query(msg, APIQueryZoneReply.class);
        Assert.assertEquals(1, reply.getInventories().size());

        msg = new APIQueryZoneMsg();
        msg.addQueryCondition("type", QueryOp.EQ, "zstack");
        msg.setGroupBy("type");
        reply = api.query(msg, APIQueryZoneReply.class);
        Assert.assertEquals(1, reply.getInventories().size());
    }
}
