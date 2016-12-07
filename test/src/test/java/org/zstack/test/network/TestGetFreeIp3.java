package org.zstack.test.network;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.network.l3.FreeIpInventory;
import org.zstack.header.network.l3.IpRangeInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.test.*;

import java.util.List;

public class TestGetFreeIp3 {
    Api api;
    ComponentLoader loader;
    DatabaseFacade dbf;
    CloudBus bus;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        BeanConstructor con = new WebBeanConstructor();
        /* This loads spring application context */
        loader = con.addXml("PortalForUnitTest.xml").addXml("ZoneManager.xml").addXml("NetworkManager.xml").addXml("AccountManager.xml").build();
        dbf = loader.getComponent(DatabaseFacade.class);
        bus = loader.getComponent(CloudBus.class);
        api = new Api();
        api.startServer();
    }


    @Test
    public void test() throws ApiSenderException {
        ZoneInventory zone = api.createZones(1).get(0);
        L2NetworkInventory linv = api.createNoVlanL2Network(zone.getUuid(), "eth0");
        L3NetworkInventory l3inv = api.createL3BasicNetwork(linv.getUuid());
        L3NetworkVO vo = dbf.findByUuid(l3inv.getUuid(), L3NetworkVO.class);
        Assert.assertNotNull(vo);
        String gw = "10.223.104.1";
        String nw = "255.255.252.0";
        IpRangeInventory ipr = api.addIpRange(l3inv.getUuid(), "10.223.104.250", "10.223.104.255", gw, nw);
        IpRangeInventory ipr2 = api.addIpRange(l3inv.getUuid(), "10.223.105.0", "10.223.105.10", gw, nw);
        List<FreeIpInventory> ips = api.getFreeIp(l3inv.getUuid(), null, 11, "10.223.104.254");
        Assert.assertEquals(11, ips.size());
        FreeIpInventory ip1 = ips.get(0);
        Assert.assertEquals(gw, ip1.getGateway());
        Assert.assertEquals(nw, ip1.getNetmask());
        ips = api.getFreeIp(l3inv.getUuid(), null, 13, "10.223.104.254");
        Assert.assertEquals(13, ips.size());
        ips = api.getFreeIp(l3inv.getUuid(), null, 13, "10.223.104.249");
        Assert.assertEquals(13, ips.size());
        ips = api.getFreeIp(l3inv.getUuid(), null, 17, "10.223.104.249");
        Assert.assertEquals(17, ips.size());
        ips = api.getFreeIp(l3inv.getUuid(), null, 13, "10.223.105.249");
        Assert.assertEquals(0, ips.size());
        ips = api.getFreeIp(null, ipr.getUuid(), 13, "10.223.104.254");
        Assert.assertEquals(2, ips.size());
        IpRangeInventory ipr3 = api.addIpRange(l3inv.getUuid(), "10.223.106.0", "10.223.109.10", gw, nw);
        ips = api.getFreeIp(l3inv.getUuid(), null, 13, "10.223.106.255");
        Assert.assertEquals(13, ips.size());
    }
}
