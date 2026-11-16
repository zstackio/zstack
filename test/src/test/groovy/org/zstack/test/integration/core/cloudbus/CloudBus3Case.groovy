package org.zstack.test.integration.core.cloudbus

import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.Logger
import org.apache.logging.log4j.core.appender.AbstractAppender
import org.apache.logging.log4j.core.config.Property
import org.apache.logging.log4j.core.layout.PatternLayout
import org.springframework.http.HttpEntity
import org.zstack.core.CoreGlobalProperty
import org.zstack.core.Platform
import org.zstack.core.cloudbus.*
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.thread.SyncThread
import org.zstack.core.thread.ThreadFacade
import org.zstack.header.AbstractService
import org.zstack.header.core.FutureCompletion
import org.zstack.header.core.progress.ChainInfo
import org.zstack.header.errorcode.OperationFailureException
import org.zstack.header.errorcode.SysErrors
import org.zstack.header.identity.APILogInByAccountMsg
import org.zstack.header.managementnode.ManagementNodeInventory
import org.zstack.header.message.APIEvent
import org.zstack.header.message.Message
import org.zstack.header.message.MessageReply
import org.zstack.header.message.NeedReplyMessage
import org.zstack.header.network.l3.APIAddDnsToL3NetworkMsg
import org.zstack.header.network.l3.CreateL3NetworkMsg
import org.zstack.header.vm.APIQueryVmInstanceMsg
import org.zstack.header.vm.APIStartVmInstanceMsg
import org.zstack.header.vm.StartVmInstanceMsg
import org.zstack.kvm.KVMHostSyncHttpCallMsg
import org.zstack.test.integration.ZStackTest
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil

import javax.servlet.http.HttpServletResponse
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

import static org.mockito.Mockito.mock
import static org.mockito.Mockito.verify

class CloudBus3Case extends SubCase {
    AbstractService service
    CloudBus bus

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

    void testHttpMessageLogSafety() {
        CloudBusIN ingress = bean(CloudBusIN.class)
        String secret = "cloudbus-log-test-${Platform.uuid}"
        List<String> warnings = Collections.synchronizedList([])
        List<Message> received = Collections.synchronizedList([])
        def layout = PatternLayout.newBuilder().withPattern("%m%n%throwable").build()
        def appender = new AbstractAppender("cloudbus-log-test", null, layout, false, Property.EMPTY_ARRAY) {
            @Override
            void append(LogEvent event) {
                if (event.level == Level.WARN && event.message.formattedMessage.startsWith("unable to")) {
                    warnings.add(layout.toSerializable(event))
                }
            }
        }
        Logger logger = (Logger) LogManager.getLogger(CloudBusImpl3.class)
        Level oldLevel = logger.level
        String serviceId = bus.makeLocalServiceId("http-log-test-${Platform.uuid}")
        def httpService = new AbstractService() {
            @Override
            void handleMessage(Message msg) {
                received.add(msg)
            }

            @Override
            String getId() { return serviceId }

            @Override
            boolean start() { return true }

            @Override
            boolean stop() { return true }
        }
        bus.registerService(httpService)
        appender.start()
        logger.addAppender(appender)
        logger.level = Level.WARN
        try {
            [
                    [("org.zstack.test.MissingCloudBusMessage"): [password: secret]],
                    [(APILogInByAccountMsg.name): [password: secret, timeout: secret]],
                    [(APILogInByAccountMsg.name): [password: secret, accountName: "cloudbus-log-test",
                                                  headers: [schema: [accountName: "missing." + secret]]]]
            ].each { body ->
                warnings.clear()
                ingress.handleHttpRequest(new HttpEntity<String>(JSONObjectUtil.toJsonString(body)),
                        mock(HttpServletResponse.class))
                retryInSecs { assert warnings.size() == 1 }
                assert warnings[0].contains("unable to parse message from HTTP")
                assert !warnings[0].contains(secret)
                assert received.empty
            }

            warnings.clear()
            ingress.handleHttpRequest(new HttpEntity<String>(JSONObjectUtil.toJsonString(
                    [(APILogInByAccountMsg.name): [password: secret, accountName: "cloudbus-log-test"]])),
                    mock(HttpServletResponse.class))
            retryInSecs { assert warnings.size() == 1 }
            assert warnings[0].contains("unable to deliver a message received from HTTP")
            assert warnings[0].contains("cloudbus-log-test")
            assert warnings[0].contains("*****")
            assert warnings[0].contains("NullPointerException")
            assert !warnings[0].contains(secret)
            assert received.empty

            warnings.clear()
            ingress.handleHttpRequest(new HttpEntity<String>(JSONObjectUtil.toJsonString(
                    [(KVMHostSyncHttpCallMsg.name): [commandClassName: APILogInByAccountMsg.name,
                                                     command: "invalid-" + secret]])),
                    mock(HttpServletResponse.class))
            retryInSecs { assert warnings.size() == 1 }
            assert warnings[0].contains("unable to deliver a message received from HTTP")
            assert warnings[0].contains("<unavailable>")
            assert warnings[0].contains("NullPointerException")
            assert !warnings[0].contains(secret)
            assert received.empty

            [APIAddDnsToL3NetworkMsg.name, CreateL3NetworkMsg.name].each { type ->
                def response = mock(HttpServletResponse.class)
                ingress.handleHttpRequest(new HttpEntity<String>(JSONObjectUtil.toJsonString(
                        [(type): [serviceId: serviceId, networkOperation: [fenceToken: "test-context"]]])), response)
                retryInSecs {
                    assert received.size() == 1
                    verify(response).setStatus(200)
                }
                assert received[0].networkOperation.fenceToken ==
                        (type == APIAddDnsToL3NetworkMsg.name ? null : "test-context")
                received.clear()
            }
        } finally {
            logger.removeAppender(appender)
            logger.level = oldLevel
            appender.stop()
            bus.unregisterService(httpService)
        }
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
            StartVmInstanceMsg msg = new StartVmInstanceMsg(vmInstanceUuid: Platform.uuid)
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

    void testManagementNodeGone() {

        CloudBus bus = bean(CloudBus.class)
        CloudBus3ManagementNodeLifeCycleTracker tracker = bean(CloudBus3ManagementNodeLifeCycleTracker.class)
        String SERVICE_ID = "nodeGone"

        def service = new AbstractService() {
            @Override
            void handleMessage(Message msg) {
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

        MessageReply errorReply
        StartVmInstanceMsg msg = new StartVmInstanceMsg(vmInstanceUuid: Platform.uuid)
        bus.makeLocalServiceId(msg, SERVICE_ID)
        bus.send(msg, new CloudBusCallBack(null) {
            @Override
            void run(MessageReply reply) {
                errorReply = reply
            }
        })

        tracker.nodeLeft(new ManagementNodeInventory(uuid: Platform.getManagementServerId()))

        retryInSecs {
            assert errorReply
            assert !errorReply.isSuccess()
            assert errorReply.getError().getCode() == SysErrors.MANAGEMENT_NODE_UNAVAILABLE_ERROR.toString()
        }

        APIEvent errorEvent
        APIStartVmInstanceMsg amsg = new APIStartVmInstanceMsg()
        bus.makeLocalServiceId(amsg, SERVICE_ID)
        bus.send(amsg, new Consumer<APIEvent>() {
            @Override
            void accept(APIEvent apiEvent) {
                errorEvent = apiEvent
            }
        })

        tracker.nodeLeft(new ManagementNodeInventory(uuid: Platform.getManagementServerId()))

        retryInSecs {
            assert errorEvent
            assert !errorEvent.isSuccess()
            assert errorEvent.getError().getCode() == SysErrors.MANAGEMENT_NODE_UNAVAILABLE_ERROR.toString()
        }

        errorReply = null
        APIQueryVmInstanceMsg qmsg = new APIQueryVmInstanceMsg()
        bus.makeLocalServiceId(qmsg, SERVICE_ID)
        bus.send(qmsg, new CloudBusCallBack(null) {
            @Override
            void run(MessageReply reply) {
                errorReply = reply
            }
        })

        tracker.iAmDead(new ManagementNodeInventory(uuid: Platform.getManagementServerId()))

        retryInSecs {
            assert errorReply
            assert !errorReply.isSuccess()
            assert errorReply.getError().getCode() == SysErrors.MANAGEMENT_NODE_UNAVAILABLE_ERROR.toString()
        }

        int cleanupInterval = CloudBusGlobalProperty.CLOUDBUS3_MESSAGE_TRACKER_CLEANUP_INTERVAL
        CloudBusGlobalProperty.CLOUDBUS3_MESSAGE_TRACKER_CLEANUP_INTERVAL = 1
        tracker.startCleanupTimer()

        qmsg = new APIQueryVmInstanceMsg()
        qmsg.setTimeout(2L)
        bus.makeLocalServiceId(qmsg, SERVICE_ID)
        bus.send(qmsg, new CloudBusCallBack(null) {
            @Override
            void run(MessageReply reply) {
            }
        })

        retryInSecs {
            assert tracker.messageTrackers.values().find { it.message.id == qmsg.id } == null
        }

        bus.unregisterService(service)
        CloudBusGlobalProperty.CLOUDBUS3_MESSAGE_TRACKER_CLEANUP_INTERVAL = cleanupInterval
    }

    void testSendToMissingNode() {
        CloudBus bus = bean(CloudBus.class)
        DatabaseFacade dbf = bean(DatabaseFacade.class)

        String SERVICE_ID = "testMissingNode"

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

        def qmsg = new APIQueryVmInstanceMsg()
        qmsg.setTimeout(1L)
        bus.makeServiceIdByManagementNodeId(qmsg, SERVICE_ID, "some-fake-uuid")

        MessageReply r = bus.call(qmsg)
        assert r.error.isError(SysErrors.TIMEOUT)

        qmsg = new APIQueryVmInstanceMsg()
        bus.makeServiceIdByManagementNodeId(qmsg, SERVICE_ID, "some-fake-uuid2")

        expect(OperationFailureException.class) {
            bus.call(qmsg)
        }

        bus.unregisterService(service)
    }

    void testFutureCompletionSend() {
        CloudBus bus = bean(CloudBus.class)
        String SERVICE_ID = "testFutureCompletionSend"

        if (service == null) {
            service = new AbstractService() {
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
        }

        StartVmInstanceMsg msg = new StartVmInstanceMsg(vmInstanceUuid: Platform.uuid)
        bus.makeLocalServiceId(msg, SERVICE_ID)
        FutureCompletion completion = bus.send(msg)
        completion.await(5000L)
        assert completion.isSuccess()
        assert !completion.isTimeout()

        MessageReply[] replyHolder = new MessageReply[1]
        NeedReplyMessage nrMsg = new StartVmInstanceMsg(vmInstanceUuid: Platform.uuid)
        bus.makeLocalServiceId(nrMsg, SERVICE_ID)
        FutureCompletion completion2 = bus.send(nrMsg, new CloudBusCallBack(null) {
            @Override
            void run(MessageReply reply) {
                replyHolder[0] = reply
            }
        })
        completion2.await(5000L)
        assert completion2.isSuccess()
        assert !completion2.isTimeout()
        retryInSecs {
            assert replyHolder[0] != null
            assert replyHolder[0].isSuccess()
        }
    }

    void testFutureCompletionSendToNonExistService() {
        CloudBus bus = bean(CloudBus.class)
        String NON_EXIST_SERVICE_ID = "NonExistService" + Platform.uuid

        StartVmInstanceMsg msg = new StartVmInstanceMsg(vmInstanceUuid: Platform.uuid)
        bus.makeLocalServiceId(msg, NON_EXIST_SERVICE_ID)
        FutureCompletion completion = bus.send(msg)
        completion.await(5000L)
        assert completion.isSuccess()


        MessageReply[] replyHolder = new MessageReply[1]
        NeedReplyMessage nrMsg = new StartVmInstanceMsg(vmInstanceUuid: Platform.uuid)
        bus.makeLocalServiceId(nrMsg, NON_EXIST_SERVICE_ID)
        FutureCompletion completion2 = bus.send(nrMsg, new CloudBusCallBack(null) {
            @Override
            void run(MessageReply reply) {
                replyHolder[0] = reply
            }
        })
        completion2.await(5000L)
        assert completion2.isSuccess()
        retryInSecs {
            assert replyHolder[0] != null
            assert !replyHolder[0].isSuccess()
        }
    }

    void testFutureCompletionSendTimeout() {
        CloudBus bus = bean(CloudBus.class)
        String SERVICE_ID = "testFutureCompletionSendTimeout"

        def service = new AbstractService() {
            @Override
            void handleMessage(Message msg) {
                // Do not reply to simulate timeout
                try {
                    Thread.sleep(3000)
                } catch (InterruptedException e) {
                    e.printStackTrace()
                }
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

        // test timeout (Message msg)
        StartVmInstanceMsg msg = new StartVmInstanceMsg(vmInstanceUuid: Platform.uuid)
        msg.setTimeout(1)
        bus.makeLocalServiceId(msg, SERVICE_ID)
        FutureCompletion completion = bus.send(msg)
        completion.await(5000L)
        assert completion.isSuccess()

        MessageReply[] replyHolder = new MessageReply[1]
        NeedReplyMessage nrMsg = new StartVmInstanceMsg(vmInstanceUuid: Platform.uuid)
        nrMsg.setTimeout(1)
        bus.makeLocalServiceId(nrMsg, SERVICE_ID)
        FutureCompletion completion2 = bus.send(nrMsg, new CloudBusCallBack(null) {
            @Override
            void run(MessageReply reply) {
                replyHolder[0] = reply
            }
        })
        completion2.await(5000L)
        assert completion2.isSuccess()
        retryInSecs {
            assert replyHolder[0] != null
            assert !replyHolder[0].isSuccess()
            assert replyHolder[0].error.isError(SysErrors.TIMEOUT)
        }

        bus.unregisterService(service)
    }

    void testFutureCompletionSendToMissingNode() {
        CloudBus bus = bean(CloudBus.class)
        String SERVICE_ID = "testFutureCompletionSendToMissingNode"

        StartVmInstanceMsg msg = new StartVmInstanceMsg(vmInstanceUuid: Platform.uuid)
        msg.setTimeout(2)
        bus.makeServiceIdByManagementNodeId(msg, SERVICE_ID, "fake-management-node-uuid")
        FutureCompletion completion = bus.send(msg)
        completion.await(5000L)
        assert !completion.isSuccess()

        MessageReply[] replyHolder = new MessageReply[1]
        NeedReplyMessage nrMsg = new StartVmInstanceMsg(vmInstanceUuid: Platform.uuid)
        nrMsg.setTimeout(2)
        bus.makeServiceIdByManagementNodeId(nrMsg, SERVICE_ID, "fake-management-node-uuid")
        FutureCompletion completion2 = bus.send(nrMsg, new CloudBusCallBack(null) {
            @Override
            void run(MessageReply reply) {
                replyHolder[0] = reply
            }
        })
        completion2.await(5000L)
        assert !completion2.isSuccess()
        assert completion2.getErrorCode() != null

        retryInSecs {
            assert replyHolder[0] != null
            assert !replyHolder[0].isSuccess()
            // TODO
            // assert replyHolder[0].error.isError(SysErrors.MANAGEMENT_NODE_UNAVAILABLE_ERROR)
        }
    }

    void testSendQueue() {
        def threads = []
        1.upto(50) { i ->
            Thread t = Thread.start {
                new MessageSender().sendMsg()
            }
            threads << t
        }

        ThreadFacade thdf = bean(ThreadFacade.class)

        ChainInfo info = thdf.getChainTaskInfo("http-send-in-queue")
        assert info.runningTask.size() <= 1
        sleep(100)
        info = thdf.getChainTaskInfo("http-send-in-queue")
        assert info.runningTask.size() <= 1

        threads.each { it.join() }
    }

    @Override
    void test() {
        bus = bean(CloudBus.class)

        testHttpMessageLogSafety()
        testStepSend()
        testManagementNodeGone()
        testSendToMissingNode()
        testFutureCompletionSend()
        testFutureCompletionSendToNonExistService()
        testFutureCompletionSendTimeout()
        testFutureCompletionSendToMissingNode()
        CloudBusGlobalProperty.HTTP_ALWAYS = true

        testFutureCompletionSend()
        testFutureCompletionSendToNonExistService()
        testFutureCompletionSendTimeout()
        testFutureCompletionSendToMissingNode()

        testSendQueue()
        bus.unregisterService(service)
        CloudBusGlobalProperty.HTTP_ALWAYS = false
    }
}
