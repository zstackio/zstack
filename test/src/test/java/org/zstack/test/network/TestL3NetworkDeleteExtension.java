package org.zstack.test.network;

import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.test.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class TestL3NetworkDeleteExtension {
    CLogger logger = Utils.getLogger(TestL3NetworkDeleteExtension.class);
    Api api;
    ComponentLoader loader;
    DatabaseFacade dbf;
    L3NetworkDeleteExtension ext;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        BeanConstructor con = new WebBeanConstructor();
        /* This loads spring application context */
        loader = con.addXml("PortalForUnitTest.xml")
                .addXml("ZoneManager.xml").addXml("NetworkManager.xml").addXml("L3NetworkDeleteExtension.xml").addXml("AccountManager.xml").build();
        dbf = loader.getComponent(DatabaseFacade.class);
        ext = loader.getComponent(L3NetworkDeleteExtension.class);
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
        L3NetworkInventory l3inv = api.createL3BasicNetwork(linv.getUuid());
        ext.setPreventDelete(true);
        try {
            api.deleteL3Network(l3inv.getUuid());
        } catch (ApiSenderException e) {
        }
        L3NetworkVO vo = dbf.findByUuid(l3inv.getUuid(), L3NetworkVO.class);
        Assert.assertNotNull(vo);

        ext.setPreventDelete(false);
        ext.setExcpectedUuid(l3inv.getUuid());
        api.deleteL3Network(l3inv.getUuid());
        vo = dbf.findByUuid(l3inv.getUuid(), L3NetworkVO.class);
        Assert.assertEquals(null, vo);
        Assert.assertTrue(ext.isBeforeCalled());
        Assert.assertTrue(ext.isAfterCalled());
    }
}
