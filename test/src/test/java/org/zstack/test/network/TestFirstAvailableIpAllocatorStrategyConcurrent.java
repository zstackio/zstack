package org.zstack.test.network;

import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.thread.AsyncThread;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.network.l3.*;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.test.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.NetworkUtils;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

public class TestFirstAvailableIpAllocatorStrategyConcurrent {
    CLogger logger = Utils.getLogger(TestFirstAvailableIpAllocatorStrategyConcurrent.class);
    Api api;
    ComponentLoader loader;
    DatabaseFacade dbf;
    CloudBus bus;
    int testNum = 10;
    CyclicBarrier barrier = new CyclicBarrier(testNum + 1);
    CountDownLatch latch = new CountDownLatch(testNum);
    String startIp = "10.223.110.10";
    String endIp = "10.223.110.109";
    long ipNum = NetworkUtils.ipv4StringToLong(endIp) - NetworkUtils.ipv4StringToLong(startIp) + 1;

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

    @AsyncThread
    private void allocate(L3NetworkInventory l3inv) throws InterruptedException, BrokenBarrierException {
        barrier.await();
        int times = (int) (ipNum / testNum);
        try {
            for (int i = 0; i < times; i++) {
                AllocateIpMsg msg = new AllocateIpMsg();
                msg.setL3NetworkUuid(l3inv.getUuid());
                msg.setServiceId(bus.makeLocalServiceId(L3NetworkConstant.SERVICE_ID));
                msg.setAllocateStrategy(L3NetworkConstant.FIRST_AVAILABLE_IP_ALLOCATOR_STRATEGY);
                MessageReply reply = bus.call(msg);
                if (!reply.isSuccess()) {
                    logger.warn(reply.getError().toString());
                    return;
                }
            }
        } finally {
            latch.countDown();
        }
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException, BrokenBarrierException {
        ZoneInventory zone = api.createZones(1).get(0);
        L2NetworkInventory linv = api.createNoVlanL2Network(zone.getUuid(), "eth0");
        L3NetworkInventory l3inv = api.createL3BasicNetwork(linv.getUuid());
        L3NetworkVO vo = dbf.findByUuid(l3inv.getUuid(), L3NetworkVO.class);
        Assert.assertNotNull(vo);
        IpRangeInventory ipInv = api.addIpRange(l3inv.getUuid(), startIp, endIp, "10.223.110.1", "255.255.255.0");
        IpRangeVO ipvo = dbf.findByUuid(ipInv.getUuid(), IpRangeVO.class);
        Assert.assertNotNull(ipvo);

        for (int i = 0; i < testNum; i++) {
            allocate(l3inv);
        }
        barrier.await();
        latch.await(120, TimeUnit.SECONDS);
        SimpleQuery<UsedIpVO> query = dbf.createQuery(UsedIpVO.class);
        long count = query.count();
        Assert.assertEquals(ipNum, count);
    }
}
