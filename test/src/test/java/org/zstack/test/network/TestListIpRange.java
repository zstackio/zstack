package org.zstack.test.network;

import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.network.l3.IpRangeInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.test.*;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.function.Function;

import java.util.List;

public class TestListIpRange {
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
        ZoneInventory zone = api.createZones(1).get(0);
        L2NetworkInventory linv = api.createNoVlanL2Network(zone.getUuid(), "eth0");
        L3NetworkInventory l3inv = api.createL3BasicNetwork(linv.getUuid());
        L3NetworkVO vo = dbf.findByUuid(l3inv.getUuid(), L3NetworkVO.class);
        Assert.assertNotNull(vo);
        api.addIpRange(l3inv.getUuid(), "10.223.110.10", "10.223.110.20", "10.223.110.1", "255.255.255.0");
        api.addIpRange(l3inv.getUuid(), "10.223.110.21", "10.223.110.30", "10.223.110.1", "255.255.255.0");
        api.addIpRange(l3inv.getUuid(), "10.223.110.31", "10.223.110.40", "10.223.110.1", "255.255.255.0");
        List<IpRangeInventory> ipinvs = api.listIpRange(null);
        Assert.assertEquals(3, ipinvs.size());
        List<String> uuids = CollectionUtils.transformToList(ipinvs, new Function<String, IpRangeInventory>() {
            @Override
            public String call(IpRangeInventory arg) {
                return arg.getUuid();
            }
        });
        for (int i = 0; i < 3; i++) {
            Assert.assertEquals(uuids.get(i), ipinvs.get(i).getUuid());
        }
    }

}
