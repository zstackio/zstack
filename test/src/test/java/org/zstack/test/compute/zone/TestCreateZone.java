package org.zstack.test.compute.zone;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.db.UpdateQuery;
import org.zstack.header.message.APIEvent;
import org.zstack.header.zone.*;
import org.zstack.portal.apimediator.PortalSystemTags;
import org.zstack.test.*;

import java.util.List;

public class TestCreateZone {
    Api api;
    ComponentLoader loader;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        BeanConstructor con = new BeanConstructor();
        /* This loads spring application context */
        loader = con.addXml("PortalForUnitTest.xml").addXml("ZoneManager.xml").addXml("AccountManager.xml").build();
        dbf = loader.getComponent(DatabaseFacade.class);
        api = new Api();
        api.startServer();
    }

    @Test
    public void test() throws ApiSenderException {
        List<ZoneInventory> zones = api.createZones(1);
        Assert.assertEquals(1, zones.size());
        SimpleQuery<ZoneVO> query = dbf.createQuery(ZoneVO.class);
        long count = query.count();
        Assert.assertEquals(1, count);

        ZoneInventory inv = zones.get(0);

        UpdateQuery q = UpdateQuery.New();
        q.entity(ZoneVO.class).set(ZoneVO_.name, "newName").condAnd(ZoneVO_.uuid, Op.EQ, inv.getUuid()).update();

        ZoneVO vo = dbf.findByUuid(inv.getUuid(), ZoneVO.class);
        Assert.assertEquals("newName", vo.getName());

        q = UpdateQuery.New();
        q.entity(ZoneVO.class).condAnd(ZoneVO_.uuid, Op.EQ, inv.getUuid()).delete();
        Assert.assertFalse(dbf.isExist(inv.getUuid(), ZoneVO.class));

        APICreateZoneMsg msg = new APICreateZoneMsg();
        msg.setName("zone");
        msg.addSystemTag(PortalSystemTags.VALIDATION_ONLY.getTagFormat());
        msg.setSession(api.getAdminSession());
        ApiSender sender = new ApiSender();
        APIEvent evt = sender.send(msg, APIEvent.class);
        Assert.assertTrue(evt.isSuccess());
    }
}
