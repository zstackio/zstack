package org.zstack.test.compute.zone;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.test.*;

import java.util.List;

/**
 * 1. create a zone
 * 2. update its information
 * <p>
 * confirm the update succeeds
 */
public class TestUpdateZone {
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
    public void test() throws ApiSenderException {
        List<ZoneInventory> zones = api.createZones(1);
        ZoneInventory zone = zones.get(0);
        zone.setName("1");
        zone.setDescription("xxx");
        zone = api.updateZone(zone);

        Assert.assertEquals("1", zone.getName());
        Assert.assertEquals("xxx", zone.getDescription());
    }
}
