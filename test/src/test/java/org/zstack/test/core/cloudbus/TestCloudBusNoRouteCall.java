package org.zstack.test.core.cloudbus;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBusIN;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.header.message.MessageReply;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.test.BeanConstructor;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.concurrent.TimeUnit;

public class TestCloudBusNoRouteCall {
    CLogger logger = Utils.getLogger(TestCloudBusNoRouteCall.class);
    ComponentLoader loader;
    CloudBusIN bus;

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

    @Before
    public void setUp() throws Exception {
        BeanConstructor con = new BeanConstructor();
        loader = con.build();
        bus = loader.getComponent(CloudBusIN.class);
    }

    @Test
    public void test() throws InterruptedException, ClassNotFoundException {
        HelloWorldMsg msg = new HelloWorldMsg();
        msg.setGreet("Hello");
        msg.setServiceId("some non-existing service");
        msg.setTimeout(TimeUnit.SECONDS.toMillis(10));
        MessageReply r = bus.call(msg);
        Assert.assertFalse(r.isSuccess());
    }
}
