package org.zstack.test.integration.core.cloudbus

import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.*;

import org.zstack.core.Platform
import org.zstack.core.cloudbus.CloudBusCallBack
import org.zstack.core.cloudbus.CloudBusGlobalProperty
import org.zstack.core.cloudbus.CloudBusIN
import org.zstack.core.errorcode.ErrorFacade
import org.zstack.core.thread.AsyncThread
import org.zstack.header.AbstractService
import org.zstack.header.errorcode.ErrorCodeList
import org.zstack.header.errorcode.SysErrors
import org.zstack.header.message.Message
import org.zstack.header.message.MessageReply
import org.zstack.testlib.SkipTestSuite
import org.zstack.testlib.SubCase

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

import static org.zstack.core.Platform.operr

/**
 * Created by heathhose on 17-3-22.
 */
@SkipTestSuite
class CloudBusCase extends SubCase{
    ErrorFacade errf
    CloudBusIN bus
    CountDownLatch latch = new CountDownLatch(1)
    boolean isSuccess = false
    String servId = "FakeServiceForCloudBusCase"
    CountDownLatch startLatch = new CountDownLatch(1)
    
    class FakeService extends AbstractService {
        @Override
        public boolean start() {
            bus.registerService(this)
            bus.activeService(this)
            startLatch.countDown()
            return true
        }

        @Override
        public boolean stop() {
            bus.deActiveService(this)
            bus.unregisterService(this)
            return true
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg instanceof HelloWorldForCloudBusCaseMsg) {
                isSuccess = true
                latch.countDown()
            } else if (msg instanceof FakeNeedReplyMessage) {
                def m = new FakeNeedReplyMessage2()
                bus.makeTargetServiceIdByResourceUuid(m, servId, Platform.getUuid())
                bus.send(m, new CloudBusCallBack(msg) {
                    @Override
                    void run(MessageReply reply) {
                        def r = new MessageReply()
                        r.setError(errf.stringToOperationError("fake last error", [reply.error]))
                        bus.reply(msg, r)
                    }
                })
            } else if (msg instanceof FakeNeedReplyMessage2) {
                def m = new FakeNeedReplyMessage3()
                bus.makeTargetServiceIdByResourceUuid(m, servId, Platform.getUuid())
                bus.send(m, new CloudBusCallBack(msg) {
                    @Override
                    void run(MessageReply reply) {
                        def r = new MessageReply()
                        r.setError(errf.stringToOperationError("fake second error", [reply.error]))
                        bus.reply(msg, r)
                    }
                })
            } else if (msg instanceof FakeNeedReplyMessage3) {
                def r = new MessageReply()
                r.setError(errf.stringToOperationError("fake first error", [org.zstack.core.Platform.operr(ORG_ZSTACK_TEST_INTEGRATION_CORE_CLOUDBUS_10000, "origin error")]))
                bus.reply(msg, r)
            } else if (msg instanceof FakeNeedReplyMessage4) {
                // no reply
            }
        }

        @Override
        public String getId() {
            return bus.makeLocalServiceId(servId)
        }

    }

    @AsyncThread
    private void startFakeService() {
        FakeService fs = new FakeService()
        fs.start()
    }

    @Override
    public void setup() {
    }

    @Override
    public void environment() {
    }

    @Override
    public void test() {
        bus = bean(CloudBusIN.class)
        errf = bean(ErrorFacade.class)
        testCloudBusSharding()
        testMessageReply()
    }

    void testCloudBusSharding(){
        HelloWorldForCloudBusCaseMsg msg = new HelloWorldForCloudBusCaseMsg()
        startFakeService() 
        startLatch.await() 

        bus.makeTargetServiceIdByResourceUuid(msg, servId, Platform.getUuid()) 
        bus.send(msg) 
        latch.await(10, TimeUnit.SECONDS) 
        assert isSuccess
    }

    void testMessageReply() {
        def msg = new FakeNeedReplyMessage()
        bus.makeTargetServiceIdByResourceUuid(msg, servId, Platform.getUuid())
        MessageReply r = bus.call(msg)
        assert r.error instanceof ErrorCodeList
        assert r.error.causes[0] instanceof ErrorCodeList
        assert r.error.causes[0].causes[0] instanceof ErrorCodeList

        CloudBusGlobalProperty.SYNC_CALL_TIMEOUT = TimeUnit.MINUTES.toMillis(1)
        def msg4 = new FakeNeedReplyMessage4()
        bus.makeTargetServiceIdByResourceUuid(msg4, servId, Platform.getUuid())
        def reply = bus.call(msg4)
        assert !reply.isSuccess()
        assert reply.getError().isError(SysErrors.TIMEOUT)

        List<Message> msgs = new ArrayList<>()
        for (final def i in (1..5)) {
            def m = new FakeNeedReplyMessage4()
            bus.makeTargetServiceIdByResourceUuid(m, servId, Platform.getUuid())
            msgs.add(m)
        }
        def messageReplies = bus.call(msgs)
        assert messageReplies.size() == 5
        for (final def rr in messageReplies) {
            assert !rr.isSuccess()
            assert rr.getError().isError(SysErrors.TIMEOUT)
        }

        CloudBusGlobalProperty.SYNC_CALL_TIMEOUT = TimeUnit.MINUTES.toMillis(15)
    }

    @Override
    public void clean() {
    }
}
