package org.zstack.test.network;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.network.l3.IpRangeInventory;
import org.zstack.header.network.l3.IpRangeVO;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.test.*;

public class TestAddIpRangeByCidr {
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

    @Test
    public void test() throws ApiSenderException {
        ZoneInventory zone = api.createZones(1).get(0);
        L2NetworkInventory linv = api.createNoVlanL2Network(zone.getUuid(), "eth0");
        L3NetworkInventory l3inv = api.createL3BasicNetwork(linv.getUuid());
        IpRangeInventory ipInv = api.addIpRangeByCidr(l3inv.getUuid(), "192.168.0.1/24");
        Assert.assertEquals("255.255.255.0", ipInv.getNetmask());
        Assert.assertEquals("192.168.0.1", ipInv.getGateway());
        Assert.assertEquals("192.168.0.2", ipInv.getStartIp());
        Assert.assertEquals("192.168.0.254", ipInv.getEndIp());
        Assert.assertEquals("192.168.0.1/24", ipInv.getNetworkCidr());
        Assert.assertTrue(dbf.isExist(ipInv.getUuid(), IpRangeVO.class));
    }

}
