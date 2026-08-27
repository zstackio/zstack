package org.zstack.test.integration.physicalserver

import org.zstack.core.Platform
import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.header.core.Completion
import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.managementnode.ManagementNodeVO
import org.zstack.header.managementnode.ManagementNodeVO_
import org.zstack.header.physicalserver.PhysicalServerCpuTopology
import org.zstack.header.physicalserver.PhysicalServerNumaNode
import org.zstack.header.physicalserver.ResourceConsumerHandle
import org.zstack.portal.managementnode.LocalCpuTopologyCollector
import org.zstack.portal.managementnode.LocalResourceControlExecutor
import org.zstack.portal.managementnode.ManagementNodePhysicalServerAdapter
import org.zstack.physicalserver.PhysicalServerResourceAssignmentGlobalConfig
import org.zstack.sdk.PhysicalServerResourceAssignmentInventory
import org.zstack.sdk.RefreshPhysicalServerResourceAssignmentsAction
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SpringSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class ManagementNodeResourceAssignmentCase extends SubCase {
    static SpringSpec springSpec = PhysicalServerTest.springSpec

    EnvSpec env
    ManagementNodePhysicalServerAdapter adapter
    LocalCpuTopologyCollector topologyCollector
    LocalResourceControlExecutor executor
    String serverUuid
    String originalResourceAssignmentEnabled

    @Override
    void setup() {
        useSpring(springSpec)
    }

    @Override
    void environment() {
        env = makeEnv {}
    }

    @Override
    void clean() {
        try {
            if (adapter != null) {
                adapter.setTestSerialNumber(null)
            }
            if (topologyCollector != null) {
                topologyCollector.clearTestTopology()
            }
            PhysicalServerTest.cleanupPhysicalServerRecords()
            env.delete()
        } finally {
            try {
                executor?.setTestMode(false)
            } finally {
                if (originalResourceAssignmentEnabled != null) {
                    PhysicalServerResourceAssignmentGlobalConfig.ENABLED.updateValue(
                            originalResourceAssignmentEnabled)
                }
            }
        }
    }

    @Override
    void test() {
        env.create {
            originalResourceAssignmentEnabled =
                    PhysicalServerResourceAssignmentGlobalConfig.ENABLED.value()
            PhysicalServerResourceAssignmentGlobalConfig.ENABLED.updateValue("true")
            adapter = bean(ManagementNodePhysicalServerAdapter.class)
            topologyCollector = bean(LocalCpuTopologyCollector.class)
            executor = bean(LocalResourceControlExecutor.class)
            assert Q.New(ManagementNodeVO.class)
                    .select(ManagementNodeVO_.serverUuid)
                    .eq(ManagementNodeVO_.uuid,
                            Platform.getManagementServerId())
                    .findValue() == null :
                    "unit tests must not associate the MN through the runner's machine identity"
            adapter.setTestSerialNumber("MN-PHYSICAL-SERVER-CASE")
            SQL.New("update ManagementNodeVO m set m.serverUuid = null " +
                    "where m.uuid = :uuid")
                    .param("uuid", Platform.getManagementServerId())
                    .execute()
            adapter.refreshAssociations()
            topologyCollector.setTestTopology(topology())
            executor.setTestMode(true)

            adapter.associateLocalNode(Platform.getManagementServerId())
            waitForLocalAssociation()

            verifyAssociationAndDefaultAssignment()
            verifyCpuAndMemoryPatch()
            verifyManagedServiceUsageAndRestart()
        }
    }

    private void waitForLocalAssociation() {
        AtomicReference<String> associated = new AtomicReference<>()
        retryInSecs {
            associated.set(Q.New(ManagementNodeVO.class)
                    .select(ManagementNodeVO_.serverUuid)
                    .eq(ManagementNodeVO_.uuid,
                            Platform.getManagementServerId())
                    .findValue())
            assert associated.get() != null :
                    "local MN serial must compose ManagementNodeVO with a PhysicalServer"
        }
        serverUuid = associated.get()
    }

    private void verifyAssociationAndDefaultAssignment() {
        String targetUuid = serverUuid
        def servers = queryPhysicalServer {
            conditions = ["uuid=${targetUuid}"]
        }
        assert servers.size() == 1 :
                "MN association must create exactly one PhysicalServer: " +
                        "serverUuid=${serverUuid} actual=${servers.size()}"
        assert servers[0].serialNumber == "mn-physical-server-case" :
                "MN association must persist the normalized machine serial: " +
                        "expected=mn-physical-server-case " +
                        "actual=${servers[0].serialNumber}"
        assert servers[0].zoneUuid == null :
                "a dedicated management node has no Zone ownership: " +
                        "actualZoneUuid=${servers[0].zoneUuid}"

        refreshPhysicalServerResourceAssignments {
            delegate.serverUuid = targetUuid
        }
        retryInSecs {
            PhysicalServerResourceAssignmentInventory current = assignment()
            assert current.state == "Synced" :
                    "MANAGEMENT default Assignment must be applied locally: " +
                            "expected=Synced actual=${current.state}"
            assert current.cpuSet == "1-3,5-7" :
                    "MANAGEMENT default must exclude the complete CPU0 CoreGroup: " +
                            "expected=1-3,5-7 actual=${current.cpuSet}"
        }

        def command = executor.lastTestCommand
        assert command.roleType == "MANAGEMENT" :
                "local executor must receive the MANAGEMENT Role identity: " +
                        "actual=${command.roleType}"
        assert command.sliceName == "zstack-management.slice" :
                "MANAGEMENT Role must use its configured systemd slice: " +
                        "actual=${command.sliceName}"
        assert command.handles*.consumerKey.unique() == [
                "management-node:${Platform.getManagementServerId()}"
        ] :
                "every management service handle must belong to the local MN: " +
                        "actual=${command.handles*.consumerKey.unique()}"
    }

    private void verifyCpuAndMemoryPatch() {
        String targetUuid = serverUuid
        updatePhysicalServerResourceAssignment {
            delegate.serverUuid = targetUuid
            roleType = "MANAGEMENT"
            cpuSet = "0-1"
            memory = SizeUnit.MEGABYTE.toByte(256)
        }
        retryInSecs {
            PhysicalServerResourceAssignmentInventory current = assignment()
            assert current.cpuSet == "0-1" :
                    "MANAGEMENT CPU PATCH must become the applied boundary: " +
                            "expected=0-1 actual=${current.cpuSet}"
            assert current.memory == SizeUnit.MEGABYTE.toByte(256) :
                    "MANAGEMENT memory PATCH must be stored in bytes: " +
                            "expected=${SizeUnit.MEGABYTE.toByte(256)} " +
                            "actual=${current.memory}"
            assert current.state == "Synced" :
                    "MANAGEMENT CPU and memory PATCH must reconcile locally: " +
                            "expected=Synced actual=${current.state}"
        }
    }

    private void verifyManagedServiceUsageAndRestart() {
        String targetUuid = serverUuid
        def services = getPhysicalServerManagedServices {
            delegate.serverUuid = targetUuid
        }.services
        def managementNode = services.find {
            it.roleType == "MANAGEMENT" &&
                    it.serviceName == "management-node"
        }
        def prometheus = services.find {
            it.roleType == "MANAGEMENT" &&
                    it.serviceName == "prometheus"
        }
        assert managementNode?.state == "RUNNING" :
                "service inventory must expose the management node process: " +
                        "actual=${managementNode}"
        assert managementNode.cpuSet == "0-1" &&
                managementNode.memoryLimit == SizeUnit.MEGABYTE.toByte(256) :
                "service inventory must report the current Role boundary: " +
                        "actualCpuSet=${managementNode.cpuSet} " +
                        "actualMemory=${managementNode.memoryLimit}"
        assert prometheus?.restartable :
                "manifest restartability must be visible to callers: " +
                        "service=prometheus actual=${prometheus}"

        refreshPhysicalServerResourceAssignments {
            delegate.serverUuid = targetUuid
            roleType = "MANAGEMENT"
            serviceNames = ["prometheus"]
        }
        List<ResourceConsumerHandle> restarted =
                executor.lastTestRestartHandles
        assert restarted*.serviceName == ["prometheus"] :
                "targeted restart must select only prometheus: " +
                        "expected=[prometheus] actual=${restarted*.serviceName}"
        assert restarted[0].value == "prometheus.service" :
                "restart must use the stable systemd unit from the manifest: " +
                        "actual=${restarted[0].value}"

        CountDownLatch auxiliaryRestart = new CountDownLatch(1)
        AtomicReference<ErrorCode> auxiliaryRestartError = new AtomicReference<>()
        adapter.restartManagedServices(
                targetUuid, false, ["vector"], new Completion(null) {
            @Override
            void success() {
                auxiliaryRestart.countDown()
            }

            @Override
            void fail(ErrorCode errorCode) {
                auxiliaryRestartError.set(errorCode)
                auxiliaryRestart.countDown()
            }
        })
        assert auxiliaryRestart.await(30, TimeUnit.SECONDS)
        assert auxiliaryRestartError.get()?.details?.contains(
                "services[vector] are not defined") :
                "the MN restart message must preserve includeAuxiliaryServices=false: " +
                        "actual=${auxiliaryRestartError.get()}"
        assert executor.lastTestRestartHandles*.serviceName == ["prometheus"] :
                "a filtered auxiliary service must not reach the local restart executor"

        RefreshPhysicalServerResourceAssignmentsAction denied =
                new RefreshPhysicalServerResourceAssignmentsAction(
                        sessionId: adminSession(),
                        serverUuid: targetUuid,
                        roleType: "MANAGEMENT",
                        serviceNames: ["management-node"])
        def deniedResult = denied.call()
        assert deniedResult.error?.details?.contains(
                "SERVICE_RESTART_NOT_ALLOWED") :
                "a non-restartable core service must never be restarted by API: " +
                        "expected=SERVICE_RESTART_NOT_ALLOWED " +
                        "actual=${deniedResult.error}"
    }

    private PhysicalServerResourceAssignmentInventory assignment() {
        List<PhysicalServerResourceAssignmentInventory> rows =
                queryPhysicalServerResourceAssignment {
            conditions = [
                    "serverUuid=${serverUuid}",
                    "roleType=MANAGEMENT"
            ]
        } as List<PhysicalServerResourceAssignmentInventory>
        assert rows.size() == 1 :
                "one PhysicalServer may have only one MANAGEMENT Assignment: " +
                        "serverUuid=${serverUuid} actual=${rows.size()}"
        return rows[0]
    }

    private static PhysicalServerCpuTopology topology() {
        PhysicalServerNumaNode node = new PhysicalServerNumaNode()
        node.nodeId = "0"
        node.onlineCpus = ["0", "1", "2", "3", "4", "5", "6", "7"]
        node.coreGroups = [
                ["0", "4"], ["1", "5"],
                ["2", "6"], ["3", "7"]
        ]
        return PhysicalServerCpuTopology.from(["0": node])
    }
}
