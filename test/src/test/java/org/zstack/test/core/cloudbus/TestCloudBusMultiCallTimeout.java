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

public class TestCloudBusMultiCallTimeout {
    CLogger logger = Utils.getLogger(TestCloudBusMultiCallTimeout.class);
    ComponentLoader loader;
    CloudBusIN bus;
    Service serv;
    int msgNum = 10;
    int ignoreMsgIndex = 5;

    public static class HelloWorldMsg extends NeedReplyMessage {
        private int index;

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }
    }

    public static class HelloWorldReply extends MessageReply {
        private int index;

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
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
                r.setIndex(hmsg.getIndex());
                if (hmsg.getIndex() != ignoreMsgIndex) {
                    bus.reply(msg, r);
                }
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
            msg.setIndex(i);
            msg.setServiceId(FakeService.class.getCanonicalName());
            msg.setTimeout(TimeUnit.SECONDS.toMillis(2));
            msgs.add(msg);
        }

        List<MessageReply> rs = bus.call(msgs);
        serv.stop();
        for (MessageReply r : rs) {
            if (!r.isSuccess()) {
                Assert.assertEquals(rs.indexOf(r), this.ignoreMsgIndex);
            }
        }
    }
}
