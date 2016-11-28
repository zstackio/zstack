package org.zstack.test.network;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.thread.AsyncThread;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;

import java.util.concurrent.CountDownLatch;

public class TestAcquireIp1 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    volatile int success;
    int num = 10;
    CountDownLatch latch = new CountDownLatch(num);

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/network/TestAcquireIp1.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);

    }

    @AsyncThread
    void acquireIp(String l3Uuid) throws ApiSenderException {
        try {
            api.acquireIp(l3Uuid);
            success++;
        } finally {
            latch.countDown();
        }
    }


    @Test
    public void test() throws ApiSenderException, InterruptedException {
        L3NetworkInventory l3nw = deployer.l3Networks.get("TestL3Network1");

        for (int i = 0; i < num; i++) {
            acquireIp(l3nw.getUuid());
        }

        latch.await();
        Assert.assertEquals(3, success);
    }
}
