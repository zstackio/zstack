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

public class TestGetFreeIp2 {
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
        String gw = "10.223.110.1";
        String nw = "255.255.255.0";
        IpRangeInventory ipr = api.addIpRange(l3inv.getUuid(), "10.223.110.10", "10.223.110.20", gw, nw);
        List<FreeIpInventory> ips = api.getFreeIp(l3inv.getUuid(), null, 11, "10.223.110.10");
        Assert.assertEquals(11, ips.size());
        FreeIpInventory ip1 = ips.get(0);
        Assert.assertEquals(gw, ip1.getGateway());
        Assert.assertEquals(nw, ip1.getNetmask());
        Assert.assertEquals(ipr.getUuid(), ip1.getIpRangeUuid());
        ips = api.getFreeIp(l3inv.getUuid(), null, 11, "10.223.110.21");
        Assert.assertEquals(0, ips.size());
        ips = api.getFreeIp(l3inv.getUuid(), null, 11, "10.223.110.20");
        Assert.assertEquals(1, ips.size());
        ips = api.getFreeIp(l3inv.getUuid(), null, 11, "10.223.110.19");
        Assert.assertEquals(2, ips.size());
        ips = api.getFreeIp(l3inv.getUuid(), null, 11, "10.223.110.9");
        Assert.assertEquals(11, ips.size());
        ips = api.getFreeIp(l3inv.getUuid(), null, 4, "10.223.110.9");
        Assert.assertEquals(4, ips.size());
        ips = api.getFreeIp(l3inv.getUuid(), null, 11, "10.223.111.9");
        Assert.assertEquals(0, ips.size());

        IpRangeInventory ipri2 = api.addIpRange(l3inv.getUuid(), "10.223.110.250", "10.223.110.255", gw, nw);
        ips = api.getFreeIp(l3inv.getUuid(), null, 11, "10.223.110.254");
        Assert.assertEquals(2, ips.size());
    }
}
