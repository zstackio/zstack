package org.zstack.test.compute.zone;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.header.zone.ZoneVO;
import org.zstack.test.*;

import java.util.List;

public class TestDeleteZone {
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
        api.deleteZone(zone.getUuid());
        SimpleQuery<ZoneVO> query = dbf.createQuery(ZoneVO.class);
        long count = query.count();
        api.stopServer();
        Assert.assertEquals(0, count);
    }
}
