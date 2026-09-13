package org.zstack.test.integration.observability

import org.apache.logging.log4j.ThreadContext
import org.zstack.header.Constants
import org.zstack.core.Platform
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.cloudbus.CloudBusCallBack
import org.zstack.core.thread.ThreadFacade
import org.zstack.header.core.execution.APIQueryExecutionMsg
import org.zstack.header.AbstractService
import org.zstack.header.message.APIEvent
import org.zstack.header.message.Message
import org.zstack.header.message.MessageReply
import org.zstack.header.message.NeedReplyMessage
import org.zstack.header.zone.APICreateZoneMsg
import org.zstack.observability.ExecutionObservabilityFacadeImpl
import org.zstack.observability.ExecutionObservabilityGlobalProperty
import org.zstack.query.APIZQLQueryMsg
import org.zstack.test.integration.ZStackTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Future
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * Integration cases for the execution observability query API.
 */
class ExecutionObservabilityApiCase extends SubCase {
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
            assert ExecutionObservabilityGlobalProperty.ENABLED
            testApiExecutionLookup()
            testCriticalPathDetailContainsOnlyLongestStageChain()
            testPreferredExecutionUuidCollisionFallsBack()
            testApiExecutionTimelineIsReadOnly()
            testReadOnlyApiIsNotObserved()
            testZqlApiIsNotObserved()
            testScheduledTaskCreatesExecution()
            testContextFreeMessageCreatesExecution()
            testContextFreeNoReplyMessageHasUnmeasuredDuration()
            testContextFreeMessageTimeoutIsObservable()
            testFireAndForgetChildDoesNotKeepApiExecutionRunning()
            testReplyObservationUsesDeliverySnapshot()
            testApiResponseClosesOutstandingChildStages()
            testQueryRejectsAmbiguousSelectors()
        }
    }

    void testPreferredExecutionUuidCollisionFallsBack() {
        ExecutionObservabilityFacadeImpl recorder = bean(ExecutionObservabilityFacadeImpl.class)
        APICreateZoneMsg first = new APICreateZoneMsg()
        APICreateZoneMsg second = new APICreateZoneMsg()
        second.id = first.id
        APICreateZoneMsg concurrent = new APICreateZoneMsg()

        try {
            recorder.recordApiRequest(first)
            recorder.recordApiRequest(first)

            APIQueryExecutionMsg query = new APIQueryExecutionMsg()
            query.apiUuid = first.id
            def repeatedObservation = recorder.queryLocal(query)
            assert repeatedObservation.size() == 1
            assert repeatedObservation[0].executionUuid == first.id

            recorder.recordApiRequest(second)
            def executions = recorder.queryLocal(query)
            assert executions.size() == 2
            assert executions*.executionUuid.toSet().size() == 2
            assert executions*.executionUuid.contains(first.id)

            CountDownLatch ready = new CountDownLatch(2)
            CountDownLatch start = new CountDownLatch(1)
            List<Thread> observers = (1..2).collect {
                Thread.start {
                    ready.countDown()
                    start.await(10, TimeUnit.SECONDS)
                    recorder.recordApiRequest(concurrent)
                }
            }
            assert ready.await(10, TimeUnit.SECONDS)
            start.countDown()
            observers.each { observer ->
                observer.join(TimeUnit.SECONDS.toMillis(10))
                assert !observer.isAlive()
            }

            APIQueryExecutionMsg concurrentQuery = new APIQueryExecutionMsg()
            concurrentQuery.apiUuid = concurrent.id
            def concurrentExecutions = recorder.queryLocal(concurrentQuery)
            assert concurrentExecutions.size() == 1
            assert concurrentExecutions[0].executionUuid == concurrent.id
        } finally {
            ThreadContext.clearAll()
        }
    }

    void testApiExecutionLookup() {
        String requestApiUuid = Platform.uuid
        String zoneUuid = Platform.uuid

        createZone {
            resourceUuid = zoneUuid
            name = "execution-observability-${requestApiUuid}"
            apiId = requestApiUuid
            sessionId = adminSession()
        }

        try {
            def summary = findSingleByApiUuid(requestApiUuid)
            assert summary.executionUuid
            assert summary.executionUuid == requestApiUuid
            assert summary.trigger.type == "API"
            assert summary.trigger.apiUuid == requestApiUuid
            assert summary.state
            assert summary.nodeUuid
            assert summary.observedAt

            def filteredByNameAndDuration = queryExecution {
                delegate.apiUuid = requestApiUuid
                triggerName = "org.zstack.header.zone.APICreateZoneMsg"
                minExecutionDurationMs = 0L
            }
            assert filteredByNameAndDuration.size() == 1
            assert filteredByNameAndDuration[0].executionUuid == summary.executionUuid

            def filteredOutByDuration = queryExecution {
                delegate.apiUuid = requestApiUuid
                minExecutionDurationMs = Long.MAX_VALUE
            }
            assert filteredOutByDuration.isEmpty()

            def detail = getExecution(summary.executionUuid as String, "summary")
            assert detail.executionUuid == summary.executionUuid
            assert detail.activeStages instanceof Collection
            assert detail.activeStages.every { it.properties.containsKey("stageUuid") && !it.properties.containsKey("stageId") }
            assert detail.properties.containsKey("partial")
            assert detail.properties.containsKey("sourceNodes")
        } finally {
            deleteZone {
                uuid = zoneUuid
                sessionId = adminSession()
            }
        }
    }

    void testCriticalPathDetailContainsOnlyLongestStageChain() {
        ExecutionObservabilityFacadeImpl recorder = bean(ExecutionObservabilityFacadeImpl.class)
        APICreateZoneMsg api = new APICreateZoneMsg()
        ContextFreeMessage longStage = new ContextFreeMessage()
        ContextFreeMessage shortStage = new ContextFreeMessage()

        try {
            recorder.recordApiRequest(api)
            ThreadContext.put(Constants.THREAD_CONTEXT_API, api.id)
            ThreadContext.put("__execution_stage_uuid__", api.id)
            recorder.recordMessageDelivery(longStage)
            Thread.sleep(20)
            ThreadContext.put("__execution_stage_uuid__", api.id)
            recorder.recordMessageDelivery(shortStage)

            APIQueryExecutionMsg query = new APIQueryExecutionMsg()
            query.apiUuid = api.id
            query.detail = "criticalPath"
            def criticalPath = recorder.queryLocal(query)[0]
            def stageUuids = criticalPath.events.findAll {
                it.type == "STAGE_STARTED"
            }*.stageUuid as Set

            assert stageUuids.contains(longStage.id)
            assert !stageUuids.contains(shortStage.id)
        } finally {
            recorder.recordApiResponse(new APIEvent(api.id))
            ThreadContext.clearAll()
        }
    }

    void testApiExecutionTimelineIsReadOnly() {
        String requestApiUuid = Platform.uuid
        String zoneUuid = Platform.uuid

        createZone {
            resourceUuid = zoneUuid
            name = "execution-observability-timeline-${requestApiUuid}"
            apiId = requestApiUuid
            sessionId = adminSession()
        }

        try {
            def summaryBefore = findSingleByApiUuid(requestApiUuid)
            def timeline = getExecution(summaryBefore.executionUuid as String, "timeline")
            assert timeline.executionUuid == summaryBefore.executionUuid
            assert timeline.events instanceof Collection
            assert timeline.events.every { !it.properties.containsKey("stageId") }

            def summaryAfter = getExecution(summaryBefore.executionUuid as String, "summary")
            assert summaryAfter.state == summaryBefore.state
            assert summaryAfter.executionUuid == summaryBefore.executionUuid
        } finally {
            deleteZone {
                uuid = zoneUuid
                sessionId = adminSession()
            }
        }
    }

    void testReadOnlyApiIsNotObserved() {
        String apiUuid = Platform.uuid
        String zoneUuid = Platform.uuid

        createZone {
            resourceUuid = zoneUuid
            name = "execution-observability-read-only-${apiUuid}"
            apiId = apiUuid
            sessionId = adminSession()
        }

        try {
            assert queryZone { conditions = ["uuid=${zoneUuid}"] }
            retryInSecs {
                List result = queryExecution {
                    triggerName = "org.zstack.header.zone.APIQueryZoneMsg"
                }
                assert result.empty
            }
        } finally {
            deleteZone {
                uuid = zoneUuid
                sessionId = adminSession()
            }
        }
    }

    void testZqlApiIsNotObserved() {
        long startedAfterMillis = System.currentTimeMillis()

        zQLQuery {
            zql = "query globalconfig limit 1"
            sessionId = adminSession()
        }

        retryInSecs {
            List result = queryExecution {
                triggerName = APIZQLQueryMsg.name
                startedAfter = startedAfterMillis.toString()
            }
            assert result.empty
        }
    }

    void testScheduledTaskCreatesExecution() {
        ThreadFacade thdf = bean(ThreadFacade.class)
        String taskName = "execution-observability-periodic-${Platform.uuid}"
        CountDownLatch ran = new CountDownLatch(1)
        long startedAfterMillis = System.currentTimeMillis()

        Future<Void> future = thdf.submitPeriodicTask(new ObservabilityPeriodicTaskCase(taskName, ran))

        try {
            assert ran.await(10, TimeUnit.SECONDS)

            retryInSecs {
                List result = queryExecution {
                    triggerType = "SCHEDULED_TASK"
                    triggerName = taskName
                    startedAfter = startedAfterMillis.toString()
                }
                assert result.any {
                    it.trigger?.name == taskName &&
                            it.executionUuid == it.taskRunUuid
                }
            }
        } finally {
            future.cancel(true)
        }
    }

    void testContextFreeMessageCreatesExecution() {
        CloudBus bus = bean(CloudBus.class)
        String serviceId = "execution-observability-message-${Platform.uuid}"
        CountDownLatch handled = new CountDownLatch(1)

        AbstractService service = new AbstractService() {
            @Override
            void handleMessage(Message msg) {
                bus.reply(msg, new MessageReply())
                handled.countDown()
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

            assert handled.await(10, TimeUnit.SECONDS)

            retryInSecs {
                List result = queryExecution {
                    messageUuid = msg.id
                }
                assert result.size() == 1
                assert result[0].trigger.type == "MESSAGE"
                assert result[0].rootMessageUuid == msg.id
                assert result[0].executionUuid == msg.id
            }
        } finally {
            bus.unregisterService(service)
        }
    }

    void testFireAndForgetChildDoesNotKeepApiExecutionRunning() {
        ExecutionObservabilityFacadeImpl recorder = bean(ExecutionObservabilityFacadeImpl.class)
        APICreateZoneMsg api = new APICreateZoneMsg()
        ContextFreeMessage child = new ContextFreeMessage()
        child.putHeaderEntry(CloudBus.HEADER_NO_NEED_REPLY_MSG, Boolean.TRUE.toString())

        try {
            recorder.recordApiRequest(api)
            ThreadContext.put(Constants.THREAD_CONTEXT_API, api.id)
            recorder.recordMessageDelivery(child)
            recorder.recordApiResponse(new APIEvent(api.id))

            APIQueryExecutionMsg query = new APIQueryExecutionMsg()
            query.apiUuid = api.id
            query.detail = "timeline"
            def inventories = recorder.queryLocal(query)
            assert inventories.size() == 1
            assert inventories[0].state == "SUCCEEDED"
            assert inventories[0].activeStages.empty
            assert inventories[0].events.find {
                it.type == "STAGE_STARTED" && it.stageUuid == child.id
            }.noReply
            assert inventories[0].events.find {
                it.type == "STAGE_SUCCEEDED" && it.stageUuid == child.id
            }.noReply
        } finally {
            ThreadContext.clearAll()
        }
    }

    void testContextFreeNoReplyMessageHasUnmeasuredDuration() {
        ExecutionObservabilityFacadeImpl recorder = bean(ExecutionObservabilityFacadeImpl.class)
        ContextFreeMessage message = new ContextFreeMessage()
        message.putHeaderEntry(CloudBus.HEADER_NO_NEED_REPLY_MSG, Boolean.TRUE.toString())

        try {
            ThreadContext.clearAll()
            recorder.recordMessageDelivery(message)

            APIQueryExecutionMsg query = new APIQueryExecutionMsg()
            query.messageUuid = message.id
            query.detail = "timeline"
            def inventories = recorder.queryLocal(query)
            assert inventories.size() == 1
            assert inventories[0].noReply
            assert inventories[0].state == "SUCCEEDED"
            assert inventories[0].events.find {
                it.type == "MESSAGE_DELIVERED"
            }.noReply
        } finally {
            ThreadContext.clearAll()
        }
    }

    void testReplyObservationUsesDeliverySnapshot() {
        ExecutionObservabilityFacadeImpl recorder = bean(ExecutionObservabilityFacadeImpl.class)
        def executorField = ExecutionObservabilityFacadeImpl.getDeclaredField("observationExecutor")
        executorField.accessible = true
        ThreadPoolExecutor executor = executorField.get(recorder) as ThreadPoolExecutor
        CountDownLatch blockerStarted = new CountDownLatch(1)
        CountDownLatch releaseBlocker = new CountDownLatch(1)
        APICreateZoneMsg api = new APICreateZoneMsg()
        ContextFreeMessage child = new ContextFreeMessage()

        try {
            recorder.recordApiRequest(api)
            ThreadContext.put(Constants.THREAD_CONTEXT_API, api.id)
            recorder.recordMessageDelivery(child)

            executor.execute {
                blockerStarted.countDown()
                releaseBlocker.await(10, TimeUnit.SECONDS)
            }
            assert blockerStarted.await(10, TimeUnit.SECONDS)

            MessageReply reply = new MessageReply()
            reply.putHeaderEntry("correlationId", child.id)
            recorder.beforeDeliveryMessage(reply)
            // CloudBus can forward the same reply instance to an outer message,
            // replacing its correlation ID before the observation worker runs.
            reply.putHeaderEntry("correlationId", api.id)
            releaseBlocker.countDown()

            retryInSecs {
                APIQueryExecutionMsg query = new APIQueryExecutionMsg()
                query.apiUuid = api.id
                query.detail = "timeline"
                def inventory = recorder.queryLocal(query)[0]
                assert inventory.activeStages.empty
                assert inventory.events.any {
                    it.type == "STAGE_SUCCEEDED" && it.stageUuid == child.id
                }
            }
        } finally {
            releaseBlocker.countDown()
            recorder.recordApiResponse(new APIEvent(api.id))
            ThreadContext.clearAll()
        }
    }

    void testApiResponseClosesOutstandingChildStages() {
        ExecutionObservabilityFacadeImpl recorder = bean(ExecutionObservabilityFacadeImpl.class)
        APICreateZoneMsg api = new APICreateZoneMsg()
        ContextFreeMessage child = new ContextFreeMessage()

        try {
            recorder.recordApiRequest(api)
            ThreadContext.put(Constants.THREAD_CONTEXT_API, api.id)
            recorder.recordMessageDelivery(child)
            recorder.recordApiResponse(new APIEvent(api.id))

            APIQueryExecutionMsg query = new APIQueryExecutionMsg()
            query.apiUuid = api.id
            query.detail = "timeline"
            def inventory = recorder.queryLocal(query)[0]
            assert inventory.state == "SUCCEEDED"
            assert inventory.activeStages.empty
            assert inventory.events.any {
                it.type == "STAGE_DETACHED" && it.stageUuid == child.id
            }
        } finally {
            ThreadContext.clearAll()
        }
    }

    void testQueryRejectsAmbiguousSelectors() {
        Throwable error
        try {
            queryExecution {
                apiUuid = Platform.uuid
                messageUuid = Platform.uuid
            }
            error = null
        } catch (AssertionError t) {
            error = t
        }

        assert error != null
        assert error.message.contains("ORG_ZSTACK_CORE_CLOUDBUS_10012")
    }

    void testContextFreeMessageTimeoutIsObservable() {
        CloudBus bus = bean(CloudBus.class)
        String serviceId = "execution-observability-timeout-${Platform.uuid}"
        CountDownLatch received = new CountDownLatch(1)

        AbstractService service = new AbstractService() {
            @Override
            void handleMessage(Message msg) {
                received.countDown()
                // Intentionally do not reply: the CloudBus timeout path should
                // close the execution as TIMEOUT without business instrumentation.
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
            msg.timeout = 100
            bus.makeLocalServiceId(msg, serviceId)
            bus.send(msg, new CloudBusCallBack(null) {
                @Override
                void run(MessageReply reply) {
                    assert !reply.success
                }
            })

            assert received.await(10, TimeUnit.SECONDS)
            retryInSecs {
                List result = queryExecution {
                    messageUuid = msg.id
                    state = "TIMEOUT"
                }
                assert result.size() == 1
                assert result[0].state == "TIMEOUT"
            }
        } finally {
            bus.unregisterService(service)
        }
    }

    private def findSingleByApiUuid(String apiId) {
        List result = queryExecution {
            delegate.apiUuid = apiId
        }
        assert result instanceof Collection
        assert result.size() == 1
        return result[0]
    }

    private def getExecution(String executionId, String detailType) {
        List result = queryExecution {
            executionUuid = executionId
            detail = detailType
        }
        assert result instanceof Collection
        assert result.size() == 1
        return result[0]
    }

    static class ContextFreeMessage extends NeedReplyMessage {
    }
}
