package org.zstack.test.compute.zone;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cascade.CascadeException;
import org.zstack.core.cascade.CascadeFacade;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.zone.ZoneEO;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.header.zone.ZoneVO;
import org.zstack.test.*;

import java.util.Arrays;
import java.util.List;

public class TestDeleteZone2 {
    Api api;
    ComponentLoader loader;
    DatabaseFacade dbf;
    CascadeFacade casf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        BeanConstructor con = new WebBeanConstructor();
        /* This loads spring application context */
        loader = con.addXml("PortalForUnitTest.xml").addXml("ZoneManager.xml").addXml("AccountManager.xml").build();
        dbf = loader.getComponent(DatabaseFacade.class);
        casf = loader.getComponent(CascadeFacade.class);
        api = new Api();
        api.startServer();
    }

    @Test
    public void test() throws ApiSenderException, CascadeException {
        List<ZoneInventory> zones = api.createZones(1);
        ZoneInventory zone = zones.get(0);
        api.deleteZone(zone.getUuid());
        SimpleQuery<ZoneVO> query = dbf.createQuery(ZoneVO.class);
        long count = query.count();
        api.stopServer();
        Assert.assertEquals(0, count);
        count = dbf.count(ZoneEO.class);
        Assert.assertEquals(1, count);
        casf.syncCascade(CascadeConstant.DELETION_DELETE_CODE, ZoneVO.class.getSimpleName(), Arrays.asList(zone));
        count = dbf.count(ZoneEO.class);
        Assert.assertEquals(0, count);
    }
}
