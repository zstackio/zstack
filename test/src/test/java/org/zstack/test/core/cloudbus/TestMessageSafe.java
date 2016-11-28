package org.zstack.test.core.cloudbus;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBusIN;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.header.AbstractService;
import org.zstack.header.Service;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.test.BeanConstructor;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.concurrent.TimeUnit;

public class TestMessageSafe {
    CLogger logger = Utils.getLogger(TestMessageSafe.class);
    ComponentLoader loader;
    CloudBusIN bus;
    Service serv;

    public static class HelloWorldMsg extends NeedReplyMessage {
        private String greet;

        public String getGreet() {
            return greet;
        }

        public void setGreet(String greet) {
            this.greet = greet;
        }

    }

    public static class HelloWorldReply extends MessageReply {
        private String greet;

        public String getGreet() {
            return greet;
        }

        public void setGreet(String greet) {
            this.greet = greet;
        }
    }

    class FakeService extends AbstractService {
        @Override
        public boolean start() {
            bus.registerService(this);
            return true;
        }

        @Override
        public boolean stop() {
            bus.unregisterService(this);
            return true;
        }

        @Override
        @MessageSafe
        public void handleMessage(Message msg) {
            throw new RuntimeException("on purpose");
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
        HelloWorldMsg msg = new HelloWorldMsg();
        msg.setGreet("Hello");
        msg.setServiceId(FakeService.class.getCanonicalName());
        msg.setTimeout(TimeUnit.SECONDS.toMillis(10));
        MessageReply r = bus.call(msg);
        serv.stop();
        Assert.assertFalse(r.isSuccess());
    }
}
