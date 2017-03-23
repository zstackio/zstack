package org.zstack.test.integration.core.cloudbus

import groovy.transform.TypeChecked
import junit.framework.Assert
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.cloudbus.CloudBusCallBack
import org.zstack.header.message.MessageReply
import org.zstack.header.message.NeedReplyMessage
import org.zstack.test.aop.CloudBusAopProxy
import org.zstack.test.core.cloudbus.FakeNeedReplyMessage
import org.zstack.testlib.SubCase
import org.zstack.utils.Utils
import org.zstack.utils.logging.CLogger

/**
 * Created by heathhose on 17-3-22.
 */
class CloudBusProxyCase extends SubCase{

    CloudBus bus
    CloudBusAopProxy aop
    boolean isSuccess = false
    List<FakeNeedReplyMessage> msgs = new ArrayList<FakeNeedReplyMessage>(2)
    FakeNeedReplyMessage msg = new FakeNeedReplyMessage()

    @Override
    void setup() {
        spring {
            include("CloudBusAopProxy.xml")
        }
    }

    @Override
    void environment() {
    }

    @Override
    void test() {
        bus = bean(CloudBus.class)
        aop = bean(CloudBusAopProxy.class)

        testBusSendMsgsWhenCloudBusAopProxyBehaviorFAIL()
        testBusSendCallBackMgsWhenCloudBusAopProxyBehaviorFAIL()
        testBusCallMsgWhenCloudBusAopProxyBehaviorFAIL()
        testBusSenMsgsWhenCloudBusAopProxyBehaviorTIMEOUT()
    }

    void testBusSendMsgsWhenCloudBusAopProxyBehaviorFAIL(){
        aop.addMessage(NeedReplyMessage.class, CloudBusAopProxy.Behavior.FAIL)

        msg.setServiceId("A fake service id not needed")
        msgs.add(msg)
        msg = new FakeNeedReplyMessage()
        msg.setServiceId("A fake service id not needed")
        msgs.add(msg)
        bus.send(msgs)
    }

    void testBusSendCallBackMgsWhenCloudBusAopProxyBehaviorFAIL(){
        msg = new FakeNeedReplyMessage()
        msg.setServiceId("A fake service id not needed")
        bus.send(msg, new CloudBusCallBack(null) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    isSuccess = true
                }
            }
        })
    }

    void testBusCallMsgWhenCloudBusAopProxyBehaviorFAIL(){
        msg = new FakeNeedReplyMessage()
        msg.setServiceId("A fake service id not needed")
        MessageReply reply = bus.call(msg)
        Assert.assertFalse(reply.isSuccess())
        Assert.assertTrue(isSuccess)
    }

    void testBusSenMsgsWhenCloudBusAopProxyBehaviorTIMEOUT(){
        aop.addMessage(NeedReplyMessage.class, CloudBusAopProxy.Behavior.TIMEOUT)

        msgs.clear()
        msg = new FakeNeedReplyMessage()
        msg.setServiceId("A fake service id not needed")
        msg.setTimeout(1)
        msgs.add(msg)
        msg = new FakeNeedReplyMessage()
        msg.setServiceId("A fake service id not needed")
        msg.setTimeout(1)
        msgs.add(msg)
        List<MessageReply> replies = bus.call(msgs)
        for (MessageReply r : replies) {
            Assert.assertFalse(r.isSuccess())
        }
    }

    @Override
    void clean() {
    }

}
