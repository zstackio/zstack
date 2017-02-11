package org.zstack.test.aop;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.header.message.MessageReply;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.test.Api;
import org.zstack.test.BeanConstructor;
import org.zstack.test.core.cloudbus.FakeNeedReplyMessage;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;

public class TestCloudBusProxy {
    CLogger logger = Utils.getLogger(TestCloudBusProxy.class);
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    CloudBusAopProxy aop;
    boolean isSuccess = false;

    @Before
    public void setUp() throws Exception {
        BeanConstructor con = new BeanConstructor();
        loader = con.addXml("PortalForUnitTest.xml").addXml("ZoneManager.xml").addXml("ClusterManager.xml").addXml("CloudBusAopProxy.xml").addXml("AccountManager.xml").build();
        aop = loader.getComponent(CloudBusAopProxy.class);
        bus = loader.getComponent(CloudBus.class);
        api = new Api();
        api.startServer();
    }

    @Test
    public void test() throws InterruptedException {
        aop.addMessage(NeedReplyMessage.class, CloudBusAopProxy.Behavior.FAIL);

        List<FakeNeedReplyMessage> msgs = new ArrayList<FakeNeedReplyMessage>(2);
        FakeNeedReplyMessage msg = new FakeNeedReplyMessage();
        msg.setServiceId("A fake service id not needed");
        msgs.add(msg);
        msg = new FakeNeedReplyMessage();
        msg.setServiceId("A fake service id not needed");
        msgs.add(msg);
        bus.send(msgs);

        msg = new FakeNeedReplyMessage();
        msg.setServiceId("A fake service id not needed");
        bus.send(msg, new CloudBusCallBack(null) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    isSuccess = true;
                }
            }
        });

        msg = new FakeNeedReplyMessage();
        msg.setServiceId("A fake service id not needed");
        MessageReply reply = bus.call(msg);
        Assert.assertFalse(reply.isSuccess());
        logger.debug(reply.getError().toString());

        Assert.assertTrue(isSuccess);

        aop.addMessage(NeedReplyMessage.class, CloudBusAopProxy.Behavior.TIMEOUT);
        msgs.clear();
        msg = new FakeNeedReplyMessage();
        msg.setServiceId("A fake service id not needed");
        msg.setTimeout(1);
        msgs.add(msg);
        msg = new FakeNeedReplyMessage();
        msg.setServiceId("A fake service id not needed");
        msg.setTimeout(1);
        msgs.add(msg);
        List<MessageReply> replies = bus.call(msgs);
        for (MessageReply r : replies) {
            Assert.assertFalse(r.isSuccess());
        }
    }
}
