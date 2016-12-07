package org.zstack.test.compute.zone;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.zone.*;
import org.zstack.test.*;

import java.util.List;

public class TestZoneChangeStateExtensionPoint {
    Api api;
    ComponentLoader loader;
    DatabaseFacade dbf;
    ZoneChnageStateExtension ext;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        BeanConstructor con = new WebBeanConstructor();
        /* This loads spring application context */
        loader = con.addXml("PortalForUnitTest.xml").addXml("ZoneManager.xml").addXml("ZoneUnitTest.xml").addXml("AccountManager.xml").build();
        dbf = loader.getComponent(DatabaseFacade.class);
        ext = loader.getComponent(ZoneChnageStateExtension.class);
        api = new Api();
        api.startServer();
    }

    @Test
    public void test() throws ApiSenderException {
        try {
            List<ZoneInventory> zones = api.createZones(1);
            ZoneInventory zone = zones.get(0);
            ext.setPreventChange(true);
            APIChangeZoneStateMsg msg = new APIChangeZoneStateMsg(zone.getUuid(), ZoneStateEvent.disable.toString());
            msg.setSession(api.getAdminSession());
            api.getApiSender().send(msg, APIChangeZoneStateEvent.class, false);
            ZoneVO vo = dbf.findByUuid(zone.getUuid(), ZoneVO.class);
            Assert.assertEquals(ZoneState.Enabled, vo.getState());

            ext.setPreventChange(false);
            ext.setExpectedCurrent(ZoneState.Enabled);
            ext.setExpectedNext(ZoneState.Disabled);
            ext.setExpectedStateEvent(ZoneStateEvent.disable);
            api.getApiSender().send(msg, APIChangeZoneStateEvent.class);
            Assert.assertTrue(ext.isBeforeCalled());
            Assert.assertTrue(ext.isAfterCalled());
            vo = dbf.findByUuid(zone.getUuid(), ZoneVO.class);
            Assert.assertEquals(ZoneState.Disabled, vo.getState());
        } finally {
            api.stopServer();
        }
    }
}
