package org.zstack.test.network;

import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.test.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class TestL2NetworkDeleteExtension {
    CLogger logger = Utils.getLogger(TestL2NetworkDeleteExtension.class);
    Api api;
    ComponentLoader loader;
    DatabaseFacade dbf;
    L2NetworkDeleteExtension ext;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        BeanConstructor con = new WebBeanConstructor();
        /* This loads spring application context */
        loader = con.addXml("PortalForUnitTest.xml")
                .addXml("ZoneManager.xml").addXml("NetworkManager.xml").addXml("L2NetworkDeleteExtension.xml").addXml("AccountManager.xml").build();
        dbf = loader.getComponent(DatabaseFacade.class);
        ext = loader.getComponent(L2NetworkDeleteExtension.class);
        api = new Api();
        api.startServer();
    }

    @After
    public void tearDown() throws Exception {
        api.stopServer();
    }

    @Test
    public void test() throws ApiSenderException {
        ZoneInventory zone = api.createZones(1).get(0);
        L2NetworkInventory linv = api.createNoVlanL2Network(zone.getUuid(), "eth0");
        L2NetworkVO vo = dbf.findByUuid(linv.getUuid(), L2NetworkVO.class);
        Assert.assertNotNull(vo);
        ext.setPreventDelete(true);
        try {
            api.deleteL2Network(linv.getUuid());
        } catch (ApiSenderException e) {
        }
        vo = dbf.findByUuid(linv.getUuid(), L2NetworkVO.class);
        Assert.assertNotNull(vo);

        ext.setPreventDelete(false);
        ext.setExcpectedUuid(linv.getUuid());
        api.deleteL2Network(linv.getUuid());
        vo = dbf.findByUuid(linv.getUuid(), L2NetworkVO.class);
        Assert.assertEquals(null, vo);
        Assert.assertTrue(ext.isBeforeCalled());
        Assert.assertTrue(ext.isAfterCalled());
    }
}
