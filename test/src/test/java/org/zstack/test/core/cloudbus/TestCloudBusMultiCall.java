package org.zstack.test.core.cloudbus;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBusIN;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.header.AbstractService;
import org.zstack.header.Service;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.test.BeanConstructor;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TestCloudBusMultiCall {
    CLogger logger = Utils.getLogger(TestCloudBusMultiCall.class);
    ComponentLoader loader;
    CloudBusIN bus;
    Service serv;
    int msgNum = 10;

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
            if (msg.getClass() == HelloWorldMsg.class) {
                HelloWorldMsg hmsg = (HelloWorldMsg) msg;
                HelloWorldReply r = new HelloWorldReply();
                r.setGreet(hmsg.getGreet());
                bus.reply(msg, r);
            }
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
            msg.setGreet("Hello");
            msg.setServiceId(FakeService.class.getCanonicalName());
            msg.setTimeout(TimeUnit.SECONDS.toMillis(2));
            msgs.add(msg);
        }

        List<MessageReply> rs = bus.call(msgs);
        serv.stop();
        for (MessageReply r : rs) {
            if (!r.isSuccess()) {
                Assert.fail();
            } else {
                HelloWorldReply hr = (HelloWorldReply) r;
                Assert.assertEquals("Hello", hr.getGreet());
            }
        }
    }
}
