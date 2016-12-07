package org.zstack.test.compute.zone;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.zone.APIDeleteZoneEvent;
import org.zstack.header.zone.APIDeleteZoneMsg;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.header.zone.ZoneVO;
import org.zstack.test.*;

import java.util.List;

public class TestZoneDeleteExtensionPoint {
    Api api;
    ComponentLoader loader;
    DatabaseFacade dbf;
    PreventZoneDeleteExtensionPoint ext;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        BeanConstructor con = new WebBeanConstructor();
        /* This loads spring application context */
        loader = con.addXml("PortalForUnitTest.xml").addXml("ZoneManager.xml").addXml("ZoneUnitTest.xml").addXml("AccountManager.xml").build();
        dbf = loader.getComponent(DatabaseFacade.class);
        ext = loader.getComponent(PreventZoneDeleteExtensionPoint.class);
        api = new Api();
        api.startServer();
    }

    @Test
    public void test() throws ApiSenderException {
        try {
            List<ZoneInventory> zones = api.createZones(1);
            ZoneInventory zone = zones.get(0);
            APIDeleteZoneMsg msg = new APIDeleteZoneMsg(zone.getUuid());
            msg.setSession(api.getAdminSession());
            api.getApiSender().send(msg, APIDeleteZoneEvent.class, false);
            SimpleQuery<ZoneVO> query = dbf.createQuery(ZoneVO.class);
            long count = query.count();
            Assert.assertEquals(1, count);
            msg.setDeletionMode(APIDeleteMessage.DeletionMode.Enforcing);
            api.getApiSender().send(msg, APIDeleteZoneEvent.class);
            count = query.count();
            Assert.assertEquals(0, count);
            Assert.assertTrue(ext.isAfterCalled());
            Assert.assertTrue(ext.isBeforeCalled());
        } finally {
            api.stopServer();
        }
    }

}
