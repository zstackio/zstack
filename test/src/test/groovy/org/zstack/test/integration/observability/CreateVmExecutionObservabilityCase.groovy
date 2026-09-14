package org.zstack.test.integration.observability

import org.zstack.core.Platform
import org.zstack.sdk.CreateVmInstanceAction
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Integration case for observing a real CreateVmInstance API.
 */
class CreateVmExecutionObservabilityCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env?.delete()
    }

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.noVmEnv()
    }

    @Override
    void test() {
        env.create {
            testCreateVmExecutionLookup()
        }
    }

    void testCreateVmExecutionLookup() {
        String apiUuid = Platform.uuid
        String vmUuid
        CountDownLatch completed = new CountDownLatch(1)
        CreateVmInstanceAction.Result actionResult

        try {
            CreateVmInstanceAction action = new CreateVmInstanceAction()
            action.apiId = apiUuid
            action.name = "execution-observability-vm-${apiUuid}"
            action.instanceOfferingUuid = env.inventoryByName("instanceOffering").uuid
            action.imageUuid = env.inventoryByName("image1").uuid
            action.l3NetworkUuids = [env.inventoryByName("l3").uuid]
            action.sessionId = adminSession()
            action.call(new org.zstack.sdk.Completion<CreateVmInstanceAction.Result>() {
                @Override
                void complete(CreateVmInstanceAction.Result result) {
                    actionResult = result
                    vmUuid = result?.value?.inventory?.uuid
                    completed.countDown()
                }
            })

            int snapshot = 0
            assert retryInSecs(600) {
                if (completed.await(0, TimeUnit.MILLISECONDS)) {
                    return true
                }
                List result = queryExecution {
                    delegate.apiUuid = apiUuid
                }
                if (result instanceof Collection && result.size() == 1) {
                    def execution = result[0]
                    def timeline = queryExecution {
                        delegate.executionUuid = execution.executionUuid
                        delegate.detail = "timeline"
                    }[0]
                    printExecutionSnapshot(snapshot++, execution, timeline)
                }
                return false
            }: "CreateVmInstance did not complete"

            assert actionResult?.error == null
            assert vmUuid

            def execution
            retryInSecs {
                List result = queryExecution {
                    delegate.apiUuid = apiUuid
                }
                assert result instanceof Collection
                assert result.size() == 1
                assert result[0].state == "SUCCEEDED"
                execution = result[0]
            }
            assert execution.executionUuid
            assert execution.executionUuid == apiUuid
            assert execution.apiUuid == apiUuid
            assert execution.trigger?.type == "API"
            assert execution.trigger?.name
            assert execution.state == "SUCCEEDED"
            assert execution.nodeUuid
            assert execution.sourceNodes instanceof Collection

            def timeline = queryExecution {
                delegate.executionUuid = execution.executionUuid
                delegate.detail = "timeline"
            }[0]
            printExecutionSnapshot(snapshot, execution, timeline)
            assert timeline.executionUuid == execution.executionUuid
            assert timeline.events instanceof Collection
            assert timeline.events.every { it.sequence != null && it.type != null }
            assert timeline.events.any { it.type == "API_ACCEPTED" }
            def httpEvents = timeline.events.findAll { it.type == "HTTP_REQUEST_SUCCEEDED" }
            assert httpEvents
            assert httpEvents.every {
                it.httpMethod && it.httpUrl && it.httpStatusCode == 200 && it.httpElapsedMs >= 0
            }

            def messageStarts = timeline.events.findAll {
                it.type == "STAGE_STARTED" && it.stageKind == "MESSAGE"
            }
            assert messageStarts
            def messageStageUuids = messageStarts*.stageUuid as Set
            assert messageStarts.every {
                it.parentStageUuid == apiUuid || messageStageUuids.contains(it.parentStageUuid)
            }: "every message stage must be connected to the API execution topology"
            assert httpEvents.every {
                messageStageUuids.contains(it.parentStageUuid)
            }: "every HTTP stage must be connected to the message that issued it"

            def terminalEvents = timeline.events.findAll {
                it.type in ["EXECUTION_SUCCEEDED", "EXECUTION_FAILED", "EXECUTION_TIMEOUT", "EXECUTION_CANCELLED"]
            }
            assert terminalEvents.size() == 1
            assert timeline.activeStages.empty
            def childStarts = timeline.events.findAll {
                it.type in ["STAGE_STARTED", "HTTP_REQUEST_STARTED"]
            }
            def childTerminalTypes = [
                    "STAGE_SUCCEEDED", "STAGE_FAILED", "STAGE_DETACHED",
                    "HTTP_REQUEST_SUCCEEDED", "HTTP_REQUEST_FAILED", "HTTP_REQUEST_DETACHED"
            ]
            assert childStarts.every { start ->
                timeline.events.any {
                    it.stageUuid == start.stageUuid && it.type in childTerminalTypes
                }
            }: "every child Message/HTTP stage must be terminal when the API execution finishes"

            long executionTerminalSequence = terminalEvents[0].sequence
            assert timeline.events.findAll {
                it.sequence > executionTerminalSequence && it.type in childTerminalTypes
            }.every {
                it.type in ["STAGE_DETACHED", "HTTP_REQUEST_DETACHED"]
            }: "child stages observed after the authoritative API event must be detached"
        } finally {
            if (vmUuid) {
                destroyVmInstance {
                    uuid = vmUuid
                    sessionId = adminSession()
                }
            }
        }
    }

    private void printExecutionSnapshot(int number, def execution, def timeline) {
        def events = timeline.events.collect { event ->
            [sequence: event.sequence, type: event.type, stageUuid: event.stageUuid,
             stageName: event.stageName, stageKind: event.stageKind,
             parentStageUuid: event.parentStageUuid, messageUuid: event.messageUuid,
             httpMethod: event.httpMethod, httpUrl: event.httpUrl,
             httpStatusCode: event.httpStatusCode, httpElapsedMs: event.httpElapsedMs]
        }
        def active = timeline.activeStages.collect { stage ->
            [stageUuid: stage.stageUuid, parentStageUuid: stage.parentStageUuid,
             name: stage.name, kind: stage.kind, state: stage.state,
             httpMethod: stage.httpMethod, httpUrl: stage.httpUrl]
        }
        println("EXECUTION_QUERY_SNAPSHOT ${number}: " + groovy.json.JsonOutput.toJson([
                executionUuid: execution.executionUuid, state: execution.state,
                elapsedMs: execution.elapsedMs, executionMs: execution.executionMs,
                downstreamWaitMs: execution.downstreamWaitMs, activeStages: active,
                events: events]))
    }
}
