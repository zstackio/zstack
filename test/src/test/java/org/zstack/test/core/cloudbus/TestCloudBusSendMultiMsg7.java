package org.zstack.test.core.cloudbus;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBusIN;
import org.zstack.core.cloudbus.CloudBusSteppingCallback;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.header.AbstractService;
import org.zstack.header.Service;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.test.BeanConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class TestCloudBusSendMultiMsg7 {
    ComponentLoader loader;
    CloudBusIN bus;
    boolean isSuccess = true;
    Service serv;
    int msgNum = 10;
    Message timeoutMsg;
    CountDownLatch latch = new CountDownLatch(msgNum);

    public static class HelloWorldMsg extends NeedReplyMessage {
    }

    class FakeService extends AbstractService {
        int count = 0;

        @Override
        public boolean start() {
            bus.registerService(this);
            bus.activeService(this);
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
            if (count == 2) {
                timeoutMsg = msg;
            } else {
                bus.reply(msg, new MessageReply());
            }
            count++;
        }

        @Override
        public String getId() {
            return this.getClass().getCanonicalName();
        }

    }

    @Before
    public void setUp() throws Exception {
        BeanConstructor con = new BeanConstructor();
        loader = con.build();
        bus = loader.getComponent(CloudBusIN.class);
        serv = new FakeService();
        serv.start();
    }

    @Test
    public void test() throws InterruptedException, ClassNotFoundException {
        List<HelloWorldMsg> msgs = new ArrayList<HelloWorldMsg>(msgNum);
        for (int i = 0; i < msgNum; i++) {
            HelloWorldMsg msg = new HelloWorldMsg();
            msg.setServiceId(FakeService.class.getCanonicalName());
            msg.setTimeout(TimeUnit.SECONDS.toMillis(2));
            msgs.add(msg);
        }

        bus.send(msgs, 2, new CloudBusSteppingCallback(null) {

            @Override
            public void run(NeedReplyMessage msg, MessageReply reply) {
                if (timeoutMsg != null && msg.getId().equals(timeoutMsg.getId())) {
                    if (!SysErrors.TIMEOUT.toString().equals(reply.getError().getCode())) {
                        isSuccess = false;
                    }
                } else {
                    if (!reply.isSuccess()) {
                        isSuccess = false;
                    }
                }

                latch.countDown();
            }
        });

        latch.await(10, TimeUnit.SECONDS);
        serv.stop();
        Assert.assertEquals(true, isSuccess);
    }
}
