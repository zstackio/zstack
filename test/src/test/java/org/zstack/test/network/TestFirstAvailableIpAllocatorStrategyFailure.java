package org.zstack.test.network;

import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.network.l3.*;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.test.*;
import org.zstack.utils.network.NetworkUtils;

public class TestFirstAvailableIpAllocatorStrategyFailure {
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

    private void takeIp(L3NetworkInventory l3inv, IpRangeInventory ipinv, String startIp, String endIp) {
        long sip = NetworkUtils.ipv4StringToLong(startIp);
        long eip = NetworkUtils.ipv4StringToLong(endIp);

        for (long lip = sip; lip <= eip; lip++) {
            String ip = NetworkUtils.longToIpv4String(lip);
            UsedIpVO vo = new UsedIpVO(ipinv.getUuid(), ip);
            vo.setUuid(Platform.getUuid());
            vo.setIpInLong(NetworkUtils.ipv4StringToLong(ip));
            vo.setL3NetworkUuid(l3inv.getUuid());
            dbf.persist(vo);
        }
    }

    @Test
    public void test() throws ApiSenderException {
        String startIp = "10.223.110.10";
        String endIp = "10.223.110.20";
        ZoneInventory zone = api.createZones(1).get(0);
        L2NetworkInventory linv = api.createNoVlanL2Network(zone.getUuid(), "eth0");
        L3NetworkInventory l3inv = api.createL3BasicNetwork(linv.getUuid());
        L3NetworkVO vo = dbf.findByUuid(l3inv.getUuid(), L3NetworkVO.class);
        Assert.assertNotNull(vo);
        IpRangeInventory ipInv = api.addIpRange(l3inv.getUuid(), startIp, endIp, "10.223.110.1", "255.255.255.0");
        IpRangeVO ipvo = dbf.findByUuid(ipInv.getUuid(), IpRangeVO.class);
        Assert.assertNotNull(ipvo);

        takeIp(l3inv, ipInv, startIp, endIp);

        AllocateIpMsg msg = new AllocateIpMsg();
        msg.setL3NetworkUuid(l3inv.getUuid());
        msg.setServiceId(bus.makeLocalServiceId(L3NetworkConstant.SERVICE_ID));
        msg.setAllocateStrategy(L3NetworkConstant.FIRST_AVAILABLE_IP_ALLOCATOR_STRATEGY);
        MessageReply reply = (AllocateIpReply) bus.call(msg);
        Assert.assertFalse(reply.isSuccess());
    }
}
