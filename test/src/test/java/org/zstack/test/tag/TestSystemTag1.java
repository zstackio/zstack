package org.zstack.test.tag;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.query.QueryOp;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.TagDefinition;
import org.zstack.header.zone.APIQueryZoneMsg;
import org.zstack.header.zone.APIQueryZoneReply;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.header.zone.ZoneVO;
import org.zstack.tag.SystemTag;
import org.zstack.tag.TagSubQueryExtension;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;

import java.util.List;

/**
 */
public class TestSystemTag1 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @TagDefinition
    public static class TestSystemTags {
        public static SystemTag big = new SystemTag("big", ZoneVO.class);
    }

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
        api.createSystemTag(zone1.getUuid(), "big", ZoneVO.class);

        APIQueryZoneMsg msg = new APIQueryZoneMsg();
        msg.addQueryCondition(TagSubQueryExtension.SYS_TAG_NAME, QueryOp.IN, "big");
        APIQueryZoneReply reply = api.query(msg, APIQueryZoneReply.class);
        List<ZoneInventory> zones = reply.getInventories();
        Assert.assertEquals(1, zones.size());
        ZoneInventory zinv = zones.get(0);
        Assert.assertEquals(zone1.getUuid(), zinv.getUuid());
        api.deleteZone(zinv.getUuid());

        long count = dbf.count(SystemTagVO.class);
        Assert.assertEquals(0, count);
    }
}
