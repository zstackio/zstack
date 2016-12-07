package org.zstack.test.compute.zone;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.header.zone.ZoneState;
import org.zstack.header.zone.ZoneStateEvent;
import org.zstack.test.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class TestChangeZoneState {
    Api api;
    ComponentLoader loader;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        BeanConstructor con = new WebBeanConstructor();
        /* This loads spring application context */
        loader = con.addXml("PortalForUnitTest.xml").addXml("ZoneManager.xml").addXml("AccountManager.xml").build();
        dbf = loader.getComponent(DatabaseFacade.class);
        api = new Api();
        api.startServer();
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        List<ZoneInventory> zones = api.createZones(1);
        ZoneInventory zone = zones.get(0);
        TimeUnit.SECONDS.sleep(1);
        ZoneInventory s1 = api.changeZoneState(zone.getUuid(), ZoneStateEvent.disable);
        Assert.assertEquals(ZoneState.Disabled.toString(), s1.getState());
        Assert.assertFalse(s1.getCreateDate().equals(s1.getLastOpDate()));
    }
}
