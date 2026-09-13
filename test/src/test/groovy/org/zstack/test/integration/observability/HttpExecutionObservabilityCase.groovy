package org.zstack.test.integration.observability

import org.springframework.http.HttpEntity
import org.zstack.core.Platform
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.cloudbus.CloudBusCallBack
import org.zstack.header.AbstractService
import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.message.Message
import org.zstack.header.message.MessageReply
import org.zstack.header.message.NeedReplyMessage
import org.zstack.header.rest.AsyncRESTCallback
import org.zstack.header.rest.RESTFacade
import org.zstack.header.rest.RESTConstant
import org.zstack.test.integration.ZStackTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.WebBeanConstructor
import org.zstack.utils.URLBuilder

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Integration coverage for HTTP child stages exposed by the execution query API.
 */
class HttpExecutionObservabilityCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env?.delete()
    }

    @Override
    void setup() {
        useSpring(ZStackTest.springSpec)
    }

    @Override
    void environment() {
        env = env {}
    }

    @Override
    void test() {
        env.create {
            testHttpRequestIsObservable()
            testFailedHttpRequestIsObservable()
            // TODO: re-enable after the test framework dispatches HEAD requests to simulators.
            // testSyncHeadIsObservable()
        }
    }

    void testHttpRequestIsObservable() {
        CloudBus bus = bean(CloudBus.class)
        RESTFacade rest = bean(RESTFacade.class)
        String serviceId = "execution-observability-http-${Platform.uuid}"
        String path = "/execution-observability-http-${Platform.uuid}"
        String url = URLBuilder.buildHttpUrl("127.0.0.1", WebBeanConstructor.port, path)
        CountDownLatch requestStarted = new CountDownLatch(1)
        CountDownLatch releaseRequest = new CountDownLatch(1)
        CountDownLatch messageCompleted = new CountDownLatch(1)
        AtomicReference<String> callbackTaskUuid = new AtomicReference<>()

        env.simulator(path) { HttpEntity<String> entity ->
            callbackTaskUuid.set(entity.headers.getFirst(RESTConstant.TASK_UUID))
            requestStarted.countDown()
            assert releaseRequest.await(10, TimeUnit.SECONDS)
            return "ok"
        }

        AbstractService service = new AbstractService() {
            @Override
            void handleMessage(Message msg) {
                rest.asyncJsonPost(url, "{}", new AsyncRESTCallback(null) {
                    @Override
                    void success(HttpEntity<String> responseEntity) {
                        bus.reply(msg, new MessageReply())
                        messageCompleted.countDown()
                    }

                    @Override
                    void fail(ErrorCode err) {
                        bus.reply(msg, new MessageReply())
                        messageCompleted.countDown()
                    }
                }, TimeUnit.SECONDS, 10)
            }

            @Override
            String getId() {
                return bus.makeLocalServiceId(serviceId)
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
        try {
            ContextFreeMessage msg = new ContextFreeMessage()
            bus.makeLocalServiceId(msg, serviceId)
            bus.send(msg, new CloudBusCallBack(null) {
                @Override
                void run(MessageReply reply) {
                    assert reply.success
                }
            })

            assert requestStarted.await(10, TimeUnit.SECONDS)
            retryInSecs {
                List result = queryExecution {
                    messageUuid = msg.id
                    minExecutionDurationMs = 0L
                }
                assert result.size() == 1
                def execution = result[0]
                def timeline = queryExecution {
                    delegate.executionUuid = execution.executionUuid
                    delegate.detail = "timeline"
                }[0]
                assert timeline.events.any { it.type == "HTTP_REQUEST_STARTED" }
                def http = timeline.events.find { it.type == "HTTP_REQUEST_STARTED" }
                assert http.httpMethod == "POST"
                assert http.httpUrl.contains(path)
                assert http.httpTaskUuid == callbackTaskUuid.get()
                assert http.stageName == "HTTP POST ${http.httpUrl}"
                assert http.stageKind == "HTTP"
                assert http.parentStageUuid
                def activeHttp = timeline.activeStages.find { it.kind == "HTTP" && it.state == "RUNNING" }
                assert activeHttp
                assert activeHttp.httpTaskUuid == callbackTaskUuid.get()
            }

            releaseRequest.countDown()
            assert messageCompleted.await(10, TimeUnit.SECONDS)
            retryInSecs {
                def execution = queryExecution {
                    messageUuid = msg.id
                }[0]
                def timeline = queryExecution {
                    delegate.executionUuid = execution.executionUuid
                    delegate.detail = "timeline"
                }[0]
                // HTTP completion is intentionally asynchronous and may be
                // processed after the parent message has already completed.
                // In that case the observer emits DETACHED instead of
                // SUCCEEDED; both are terminal states for this child stage.
                def http = timeline.events.find {
                    it.type == "HTTP_REQUEST_SUCCEEDED" || it.type == "HTTP_REQUEST_DETACHED"
                }
                assert http
                assert http.httpTaskUuid == callbackTaskUuid.get()
                assert http.httpElapsedMs >= 0
                if (http.type == "HTTP_REQUEST_SUCCEEDED") {
                    assert http.httpStatusCode == 200
                } else {
                    assert http.details == "execution completed before a reply was observed"
                }
                assert !timeline.activeStages.any {
                    it.kind == "HTTP" && it.httpTaskUuid == callbackTaskUuid.get()
                }
            }
        } finally {
            releaseRequest.countDown()
            bus.unregisterService(service)
        }
    }

    void testFailedHttpRequestIsObservable() {
        CloudBus bus = bean(CloudBus.class)
        RESTFacade rest = bean(RESTFacade.class)
        String serviceId = "execution-observability-http-failure-${Platform.uuid}"
        // Nothing listens on this port in the test environment. The failed
        // callback must still close the HTTP child stage.
        String url = "http://127.0.0.1:1/execution-observability-http-failure"
        CountDownLatch callbackFailed = new CountDownLatch(1)
        CountDownLatch messageCompleted = new CountDownLatch(1)

        AbstractService service = new AbstractService() {
            @Override
            void handleMessage(Message msg) {
                rest.asyncJsonPost(url, "{}", new AsyncRESTCallback(null) {
                    @Override
                    void success(HttpEntity<String> responseEntity) {
                        bus.reply(msg, new MessageReply())
                        messageCompleted.countDown()
                    }

                    @Override
                    void fail(ErrorCode err) {
                        bus.reply(msg, new MessageReply())
                        callbackFailed.countDown()
                        messageCompleted.countDown()
                    }
                }, TimeUnit.SECONDS, 3)
            }

            @Override
            String getId() {
                return bus.makeLocalServiceId(serviceId)
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
        try {
            ContextFreeMessage msg = new ContextFreeMessage()
            bus.makeLocalServiceId(msg, serviceId)
            bus.send(msg, new CloudBusCallBack(null) {
                @Override
                void run(MessageReply reply) {
                    assert reply.success
                }
            })

            assert callbackFailed.await(10, TimeUnit.SECONDS)
            assert messageCompleted.await(10, TimeUnit.SECONDS)
            retryInSecs {
                def execution = queryExecution {
                    messageUuid = msg.id
                }[0]
                def timeline = queryExecution {
                    delegate.executionUuid = execution.executionUuid
                    delegate.detail = "timeline"
                }[0]
                // A connection-refused callback can complete after the parent
                // message has already finished. In that race the observer
                // records DETACHED instead of FAILED; both are terminal states.
                def failedHttp = timeline.events.find {
                    it.type == "HTTP_REQUEST_FAILED" || it.type == "HTTP_REQUEST_DETACHED"
                }
                assert failedHttp
                assert failedHttp.stageKind == "HTTP"
                assert failedHttp.httpMethod == "POST"
                assert failedHttp.httpUrl.contains("execution-observability-http-failure")
                assert failedHttp.httpElapsedMs >= 0
                if (failedHttp.type == "HTTP_REQUEST_DETACHED") {
                    assert failedHttp.details == "execution completed before a reply was observed"
                }
            }
        } finally {
            bus.unregisterService(service)
        }
    }

    void testSyncHeadIsObservable() {
        CloudBus bus = bean(CloudBus.class)
        RESTFacade rest = bean(RESTFacade.class)
        String serviceId = "execution-observability-http-head-${Platform.uuid}"
        String path = "/execution-observability-http-head-${Platform.uuid}"
        String url = URLBuilder.buildHttpUrl("127.0.0.1", WebBeanConstructor.port, path)
        CountDownLatch requestReceived = new CountDownLatch(1)
        CountDownLatch messageCompleted = new CountDownLatch(1)

        env.simulator(path) { HttpEntity<String> entity ->
            requestReceived.countDown()
            return "ok"
        }

        AbstractService service = new AbstractService() {
            @Override
            void handleMessage(Message msg) {
                try {
                    rest.syncHead(url)
                    bus.reply(msg, new MessageReply())
                } catch (Throwable t) {
                    bus.reply(msg, new MessageReply())
                } finally {
                    messageCompleted.countDown()
                }
            }

            @Override
            String getId() {
                return bus.makeLocalServiceId(serviceId)
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
        try {
            ContextFreeMessage msg = new ContextFreeMessage()
            bus.makeLocalServiceId(msg, serviceId)
            bus.send(msg, new CloudBusCallBack(null) {
                @Override
                void run(MessageReply reply) {
                    assert reply.success
                }
            })

            assert requestReceived.await(10, TimeUnit.SECONDS)
            assert messageCompleted.await(10, TimeUnit.SECONDS)
            retryInSecs {
                def execution = queryExecution {
                    messageUuid = msg.id
                }[0]
                def timeline = queryExecution {
                    delegate.executionUuid = execution.executionUuid
                    delegate.detail = "timeline"
                }[0]
                def head = timeline.events.find { it.type == "HTTP_REQUEST_SUCCEEDED" && it.httpMethod == "HEAD" }
                assert head
                assert head.httpUrl.contains(path)
                assert head.httpStatusCode == 200
            }
        } finally {
            bus.unregisterService(service)
        }
    }

    static class ContextFreeMessage extends NeedReplyMessage {
    }
}
