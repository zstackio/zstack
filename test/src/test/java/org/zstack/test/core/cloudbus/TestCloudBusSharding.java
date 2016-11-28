package org.zstack.test.core.cloudbus;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBusIN;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.thread.AsyncThread;
import org.zstack.header.AbstractService;
import org.zstack.header.message.Message;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.BeanConstructor;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class TestCloudBusSharding {
    Api api;
    ComponentLoader loader;
    DatabaseFacade dbf;
    CloudBusIN bus;
    CountDownLatch latch = new CountDownLatch(1);
    boolean isSuccess = false;
    String servId = "FakeService";
    CountDownLatch startLatch = new CountDownLatch(1);

    public static class HelloWorldMsg extends Message {
    }

    class FakeService extends AbstractService {
        @Override
        public boolean start() {
            bus.registerService(this);
            bus.activeService(this);
            startLatch.countDown();
            return true;
        }

        @Override
        public boolean stop() {
            bus.deActiveService(this);
            bus.unregisterService(this);
            return true;
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.getClass() == HelloWorldMsg.class) {
                isSuccess = true;
            }
            latch.countDown();
        }

        @Override
        public String getId() {
            return bus.makeLocalServiceId(servId);
        }

    }

    @AsyncThread
    private void startFakeService() {
        FakeService fs = new FakeService();
        fs.start();
    }

    @Before
    public void setUp() throws Exception {
        BeanConstructor con = new BeanConstructor();
        /* This loads spring application context */
        loader = con.addXml("PortalForUnitTest.xml").addXml("AccountManager.xml").build();
        dbf = loader.getComponent(DatabaseFacade.class);
        bus = loader.getComponent(CloudBusIN.class);
        api = new Api();
        startFakeService();
        startLatch.await();
        api.startServer();
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        HelloWorldMsg msg = new HelloWorldMsg();
        bus.makeTargetServiceIdByResourceUuid(msg, servId, Platform.getUuid());
        bus.send(msg);
        latch.await(10, TimeUnit.SECONDS);
        Assert.assertEquals(true, isSuccess);
    }
}
