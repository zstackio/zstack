package org.zstack.test.integration.core.cloudbus

import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.cloudbus.CloudBusListCallBack
import org.zstack.core.cloudbus.CloudBusSteppingCallback
import org.zstack.header.AbstractService
import org.zstack.header.core.FutureCompletion
import org.zstack.header.message.Message
import org.zstack.header.message.MessageReply
import org.zstack.header.message.NeedReplyMessage
import org.zstack.header.vm.StartVmInstanceMsg
import org.zstack.test.integration.ZStackTest
import org.zstack.testlib.SubCase

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class CloudBus3Case extends SubCase {
    @Override
    void clean() {
    }

    @Override
    void setup() {
        useSpring(ZStackTest.springSpec)
    }

    @Override
    void environment() {
    }

    void testStepSend() {
        CloudBus bus = bean(CloudBus.class)
        String SERVICE_ID = "testStepSend"

        def service = new AbstractService() {
            @Override
            void handleMessage(Message msg) {
                bus.reply(msg, new MessageReply())
            }

            @Override
            String getId() {
                return bus.makeLocalServiceId(SERVICE_ID)
            }

            @Override
            boolean start() {
                return true
            }

            @Override
            boolean stop() {
                return true
            }
        }

        bus.registerService(service)

        List<StartVmInstanceMsg> msgs = []
        for (int i=0; i<3; i++) {
            StartVmInstanceMsg msg = new StartVmInstanceMsg()
            bus.makeLocalServiceId(msg, SERVICE_ID)
            msgs.add(msg)
        }

        int count = 0
        CountDownLatch latch = new CountDownLatch(3)
        bus.send(msgs, 2, new CloudBusSteppingCallback(null) {
            @Override
            synchronized void run(NeedReplyMessage msg, MessageReply reply) {
                count ++
                latch.countDown()
            }
        })

        latch.await(5, TimeUnit.SECONDS)
        assert count == 3

        bus.unregisterService(service)
    }

    @Override
    void test() {
        testStepSend()
    }
}
