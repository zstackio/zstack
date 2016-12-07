package org.zstack.test.network;

import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.network.l3.*;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.test.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.NetworkUtils;

public class TestRandomIpAllocatorStrategy3 {
    CLogger logger = Utils.getLogger(TestRandomIpAllocatorStrategy3.class);
    Api api;
    ComponentLoader loader;
    DatabaseFacade dbf;
    CloudBus bus;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        BeanConstructor con = new WebBeanConstructor();
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

    @Test
    public void test() throws ApiSenderException {
        ZoneInventory zone = api.createZones(1).get(0);
        L2NetworkInventory linv = api.createNoVlanL2Network(zone.getUuid(), "eth0");
        L3NetworkInventory l3inv = api.createL3BasicNetwork(linv.getUuid());
        L3NetworkVO vo = dbf.findByUuid(l3inv.getUuid(), L3NetworkVO.class);
        Assert.assertNotNull(vo);
        String startIp = "10.223.110.10";
        String endIp = "10.223.110.20";
        IpRangeInventory ipInv = api.addIpRange(l3inv.getUuid(), startIp, endIp, "10.223.110.1", "255.255.255.0");
        IpRangeVO ipvo = dbf.findByUuid(ipInv.getUuid(), IpRangeVO.class);
        Assert.assertNotNull(ipvo);

        for (long i = 0; i < 100; i++) {
            AllocateIpMsg msg = new AllocateIpMsg();
            msg.setL3NetworkUuid(l3inv.getUuid());
            msg.setServiceId(bus.makeLocalServiceId(L3NetworkConstant.SERVICE_ID));
            msg.setAllocateStrategy(L3NetworkConstant.RANDOM_IP_ALLOCATOR_STRATEGY);
            AllocateIpReply reply = (AllocateIpReply) bus.call(msg);
            UsedIpInventory uinv = reply.getIpInventory();
            logger.debug(String.format("ip: %s, range: [%s - %s]", uinv.getIp(), startIp, endIp));
            Assert.assertTrue(NetworkUtils.isIpv4InRange(uinv.getIp(), startIp, endIp));

            ReturnIpMsg rmsg = new ReturnIpMsg();
            rmsg.setL3NetworkUuid(l3inv.getUuid());
            rmsg.setUsedIpUuid(uinv.getUuid());
            rmsg.setServiceId(bus.makeLocalServiceId(L3NetworkConstant.SERVICE_ID));
            bus.send(rmsg);
        }
    }
}
