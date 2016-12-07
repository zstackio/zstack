package org.zstack.test.network;

import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.network.l3.*;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.test.*;
import org.zstack.utils.network.NetworkUtils;

public class TestFirstAvailableIpAllocatorStrategy2 {
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

    @After
    public void tearDown() throws Exception {
        api.stopServer();
    }

    private void takeIp(L3NetworkInventory l3inv, IpRangeInventory ipinv, String... ips) {
        for (String ip : ips) {
            UsedIpVO vo = new UsedIpVO(ipinv.getUuid(), ip);
            vo.setUuid(Platform.getUuid());
            vo.setIpInLong(NetworkUtils.ipv4StringToLong(ip));
            vo.setL3NetworkUuid(l3inv.getUuid());
            dbf.persist(vo);
        }
    }

    @Test
    public void test() throws ApiSenderException {
        ZoneInventory zone = api.createZones(1).get(0);
        L2NetworkInventory linv = api.createNoVlanL2Network(zone.getUuid(), "eth0");
        L3NetworkInventory l3inv = api.createL3BasicNetwork(linv.getUuid());
        L3NetworkVO vo = dbf.findByUuid(l3inv.getUuid(), L3NetworkVO.class);
        Assert.assertNotNull(vo);
        IpRangeInventory ipInv = api.addIpRange(l3inv.getUuid(), "10.223.110.10", "10.223.110.20", "10.223.110.1", "255.255.255.0");
        IpRangeVO ipvo = dbf.findByUuid(ipInv.getUuid(), IpRangeVO.class);
        Assert.assertNotNull(ipvo);

        takeIp(l3inv, ipInv, "10.223.110.10", "10.223.110.11", "10.223.110.12", "10.223.110.14");

        AllocateIpMsg msg = new AllocateIpMsg();
        msg.setL3NetworkUuid(l3inv.getUuid());
        msg.setServiceId(bus.makeLocalServiceId(L3NetworkConstant.SERVICE_ID));
        msg.setAllocateStrategy(L3NetworkConstant.FIRST_AVAILABLE_IP_ALLOCATOR_STRATEGY);
        AllocateIpReply reply = (AllocateIpReply) bus.call(msg);
        UsedIpInventory uinv = reply.getIpInventory();
        Assert.assertEquals("10.223.110.13", uinv.getIp());
        UsedIpVO uvo = dbf.findByUuid(uinv.getUuid(), UsedIpVO.class);
        Assert.assertNotNull(uvo);
    }
}
