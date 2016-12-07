package org.zstack.test.compute.zone;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.test.*;

import java.util.ArrayList;
import java.util.List;

public class TestListZone {
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
        try {
            api.createZones(10);
            List<ZoneInventory> invs = api.listZones(null);
            Assert.assertEquals(10, invs.size());
            List<String> uuids = new ArrayList<String>(5);
            for (int i = 0; i < 5; i++) {
                uuids.add(invs.get(i).getUuid());
            }
            invs = api.listZones(uuids);
            Assert.assertEquals(5, invs.size());
            for (int i = 0; i < 5; i++) {
                Assert.assertEquals(uuids.get(i), invs.get(i).getUuid());
            }
        } finally {
            api.stopServer();
        }
    }

}
