package org.zstack.test.network;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.network.l3.*;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.test.*;

public class TestCheckIp {
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

        // gateway is not allocatable
        boolean avail = api.checkIpAvailability(l3inv.getUuid(), gw);
        Assert.assertFalse(avail);

        String testIp = "10.223.110.10";
        avail = api.checkIpAvailability(l3inv.getUuid(), testIp);
        Assert.assertTrue(avail);

        AllocateIpMsg amsg = new AllocateIpMsg();
        amsg.setRequiredIp(testIp);
        amsg.setL3NetworkUuid(l3inv.getUuid());
        bus.makeTargetServiceIdByResourceUuid(amsg, L3NetworkConstant.SERVICE_ID, amsg.getL3NetworkUuid());
        AllocateIpReply r = (AllocateIpReply) bus.call(amsg);

        avail = api.checkIpAvailability(l3inv.getUuid(), testIp);
        Assert.assertFalse(avail);

        ReturnIpMsg rmsg = new ReturnIpMsg();
        rmsg.setUsedIpUuid(r.getIpInventory().getUuid());
        rmsg.setL3NetworkUuid(r.getIpInventory().getL3NetworkUuid());
        bus.makeLocalServiceId(rmsg, L3NetworkConstant.SERVICE_ID);
        bus.call(rmsg);

        avail = api.checkIpAvailability(l3inv.getUuid(), testIp);
        Assert.assertTrue(avail);
    }
}
