package org.zstack.test.aop;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.With;
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

/**
 */
public class TestWith1 {
    CLogger logger = Utils.getLogger(TestWith1.class);
    boolean success;
    ComponentLoader loader;
    CloudBusIN bus;

    @Before
    public void setUp() throws Exception {
        BeanConstructor con = new BeanConstructor();
        loader = con.build();
        bus = loader.getComponent(CloudBusIN.class);
    }

    public static class TestMsg extends NeedReplyMessage {
    }

    @Test
    public void test() throws InterruptedException {
        Service serv = new AbstractService() {
            @Override
            public void handleMessage(final Message msg) {
                new With(msg).run(new Runnable() {
                    @Override
                    public void run() {
                        throw new RuntimeException("on purpose");
                    }
                });
            }

            @Override
            public String getId() {
                return "Test1";
            }

            @Override
            public boolean start() {
                return true;
            }

            @Override
            public boolean stop() {
                return true;
            }
        };
        bus.registerService(serv);
        bus.activeService(serv);

        TestMsg msg = new TestMsg();
        msg.setServiceId("Test1");
        msg.setTimeout(500);
        MessageReply reply = bus.call(msg);

        Assert.assertFalse(reply.isSuccess());
    }

}
