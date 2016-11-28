package org.zstack.test.tag;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.query.QueryOp;
import org.zstack.header.tag.TagInventory;
import org.zstack.header.zone.APIQueryZoneMsg;
import org.zstack.header.zone.APIQueryZoneReply;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.header.zone.ZoneVO;
import org.zstack.tag.TagSubQueryExtension;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;

import java.util.List;

/**
 */
public class TestUserTag {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/tag/TestUserTag.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException {
        ZoneInventory zone1 = deployer.zones.get("Zone1");
        TagInventory inv = api.createUserTag(zone1.getUuid(), "zone", ZoneVO.class);
        inv = api.createUserTag(zone1.getUuid(), "big", ZoneVO.class);

        APIQueryZoneMsg msg = new APIQueryZoneMsg();
        msg.addQueryCondition(TagSubQueryExtension.USER_TAG_NAME, QueryOp.IN, "zone");
        APIQueryZoneReply reply = api.query(msg, APIQueryZoneReply.class);
        List<ZoneInventory> zones = reply.getInventories();
        Assert.assertEquals(1, zones.size());
        ZoneInventory zinv = zones.get(0);
        Assert.assertEquals(zone1.getUuid(), zinv.getUuid());

        msg = new APIQueryZoneMsg();
        msg.addQueryCondition(TagSubQueryExtension.USER_TAG_NAME, QueryOp.IN, "zone", "big");
        reply = api.query(msg, APIQueryZoneReply.class);
        zones = reply.getInventories();
        Assert.assertEquals(1, zones.size());
        zinv = zones.get(0);
        Assert.assertEquals(zone1.getUuid(), zinv.getUuid());

        msg = new APIQueryZoneMsg();
        msg.addQueryCondition(TagSubQueryExtension.USER_TAG_NAME, QueryOp.NOT_IN, "zone");
        reply = api.query(msg, APIQueryZoneReply.class);
        zones = reply.getInventories();
        Assert.assertEquals(4, zones.size());

        msg = new APIQueryZoneMsg();
        msg.addQueryCondition(TagSubQueryExtension.USER_TAG_NAME, QueryOp.NOT_IN, "none existing tag");
        reply = api.query(msg, APIQueryZoneReply.class);
        zones = reply.getInventories();
        Assert.assertEquals(5, zones.size());
    }
}
