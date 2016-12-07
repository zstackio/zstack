package org.zstack.test.network;

import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.test.*;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import java.util.List;

public class TestListL2NoVlanNetwork {
    CLogger logger = Utils.getLogger(TestListL2NoVlanNetwork.class);
    Api api;
    ComponentLoader loader;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        BeanConstructor con = new WebBeanConstructor();
        /* This loads spring application context */
        loader = con.addXml("PortalForUnitTest.xml").addXml("ZoneManager.xml").addXml("NetworkManager.xml").addXml("AccountManager.xml").build();
        dbf = loader.getComponent(DatabaseFacade.class);
        api = new Api();
        api.startServer();
    }

    @After
    public void tearDown() throws Exception {
        api.stopServer();
    }

    @Test
    public void test() throws ApiSenderException {
        int testNum = 10;
        ZoneInventory zone = api.createZones(1).get(0);
        for (int i = 0; i < testNum; i++) {
            api.createNoVlanL2Network(zone.getUuid(), "eth" + i);
        }
        List<L2NetworkInventory> invs = api.listL2Network(null);
        Assert.assertEquals(testNum, invs.size());
        List<String> uuids = CollectionUtils.transformToList(invs, new Function<String, L2NetworkInventory>() {
            @Override
            public String call(L2NetworkInventory arg) {
                return arg.getUuid();
            }
        });
        invs = api.listL2Network(uuids);
        for (int i = 0; i < testNum; i++) {
            Assert.assertEquals(uuids.get(i), invs.get(i).getUuid());
        }
    }
}
