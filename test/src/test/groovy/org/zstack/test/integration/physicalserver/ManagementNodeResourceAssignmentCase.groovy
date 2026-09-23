package org.zstack.test.integration.physicalserver

import org.springframework.transaction.support.TransactionSynchronizationManager
import org.zstack.core.Platform
import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.header.managementnode.ManagementNodeVO
import org.zstack.header.managementnode.ManagementNodeVO_
import org.zstack.header.core.Completion
import org.zstack.header.physicalserver.PhysicalServerManager
import org.zstack.header.physicalserver.PhysicalServerCpuTopology
import org.zstack.header.physicalserver.PhysicalServerNumaNode
import org.zstack.header.physicalserver.ResourceConsumerHandle
import org.zstack.header.physicalserver.ResourceControlCommand
import org.zstack.portal.managementnode.ApplyManagementNodeResourceControlMsg
import org.zstack.portal.managementnode.CollectManagementNodeManagedServicesMsg
import org.zstack.portal.managementnode.LocalCpuTopologyCollector
import org.zstack.portal.managementnode.LocalResourceControlExecutor
import org.zstack.portal.managementnode.ManagementNodeResourceAssignmentFactory
import org.zstack.portal.managementnode.ReleaseManagementNodeResourceControlMsg
import org.zstack.portal.managementnode.RestartManagementNodeManagedServicesMsg
import org.zstack.physicalserver.PhysicalServerResourceAssignmentGlobalConfig
import org.zstack.sdk.PhysicalServerResourceAssignmentInventory
import org.zstack.sdk.RestartPhysicalServerManagedServicesAction
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SpringSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

import java.util.concurrent.atomic.AtomicReference

import static org.mockito.Mockito.*

class ManagementNodeResourceAssignmentCase extends SubCase {
    static SpringSpec springSpec = PhysicalServerTest.springSpec

    EnvSpec env
    ManagementNodeResourceAssignmentFactory adapter
    PhysicalServerManager physicalServerManager
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
        if (adapter != null) {
            if (physicalServerManager != null) {
                adapter.physicalServerManager = physicalServerManager
            }
            adapter.setTestSerialNumber(null)
        }
        if (topologyCollector != null) {
            topologyCollector.clearTestTopology()
        }
        PhysicalServerTest.cleanupPhysicalServerRecords()
        env.delete()
        executor?.disableTestMode()
        if (originalResourceAssignmentEnabled != null) {
            PhysicalServerResourceAssignmentGlobalConfig.ENABLED.updateValue(originalResourceAssignmentEnabled)
        }
    }

    @Override
    void test() {
        env.create {
            originalResourceAssignmentEnabled = PhysicalServerResourceAssignmentGlobalConfig.ENABLED.value()
            PhysicalServerResourceAssignmentGlobalConfig.ENABLED.updateValue("true")
            adapter = bean(ManagementNodeResourceAssignmentFactory.class)
            physicalServerManager = bean(PhysicalServerManager.class)
            def manager = spy(physicalServerManager)
            doAnswer { invocation ->
                assert !TransactionSynchronizationManager.isActualTransactionActive() :
                        "MANAGEMENT Refresh must be dispatched after the association transaction commits"
                invocation.callRealMethod()
            }.when(manager).refreshResourceAssignment(anyString(), eq("MANAGEMENT"), any(Completion.class))
            adapter.physicalServerManager = manager
            topologyCollector = bean(LocalCpuTopologyCollector.class)
            executor = bean(LocalResourceControlExecutor.class)
            assert Q.New(ManagementNodeVO.class)
                    .select(ManagementNodeVO_.serverUuid)
                    .eq(ManagementNodeVO_.uuid, Platform.getManagementServerId()).findValue() == null :
                    "unit tests must not associate the MN through the runner's machine identity"
            adapter.setTestSerialNumber("MN-PHYSICAL-SERVER-CASE")
            SQL.New("update ManagementNodeVO m set m.serverUuid = null " +
                    "where m.uuid = :uuid").param("uuid", Platform.getManagementServerId()).execute()
            topologyCollector.setTestTopology(topology())
            executor.enableTestMode()

            adapter.associateLocalNode(Platform.getManagementServerId())
            waitForLocalAssociation()

            verifyAssociationAndDefaultAssignment()
            def receiver = spy(adapter)
            doThrow(new IllegalStateException("remote MN must not read Profile")).when(receiver).roleServices()
            env.message(ApplyManagementNodeResourceControlMsg) { msg, bus ->
                assert msg.command.sliceName == "zstack-management.slice" :
                        "Apply must carry sender-resolved slice: ${msg.command.sliceName}"
                receiver.handleMessage(msg)
            }
            env.message(CollectManagementNodeManagedServicesMsg) { msg, bus ->
                assert msg.sliceName == "zstack-management.slice" :
                        "Get Services must carry sender-resolved slice: ${msg.sliceName}"
                assert msg.handles*.serviceName.contains("management-node") :
                        "Get Services must carry sender-resolved services: ${msg.handles*.serviceName}"
                receiver.handleMessage(msg)
            }
            env.message(RestartManagementNodeManagedServicesMsg) { msg, bus ->
                assert msg.sliceName == "zstack-management.slice" :
                        "Restart must carry sender-resolved slice: ${msg.sliceName}"
                receiver.handleMessage(msg)
            }
            verifyCpuAndMemoryPatch()
            verifyManagedServiceUsageAndRestart()
            verifyDisabledRelease(receiver)
            verify(receiver, never()).roleServices()
        }
    }

    private void waitForLocalAssociation() {
        AtomicReference<String> associated = new AtomicReference<>()
        retryInSecs {
            associated.set(Q.New(ManagementNodeVO.class)
                    .select(ManagementNodeVO_.serverUuid)
                    .eq(ManagementNodeVO_.uuid, Platform.getManagementServerId()).findValue())
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
                        "expected=mn-physical-server-case " + "actual=${servers[0].serialNumber}"
        assert servers[0].zoneUuid == null :
                "a dedicated management node has no Zone ownership: " + "actualZoneUuid=${servers[0].zoneUuid}"

        refreshPhysicalServerResourceAssignmentsFromProfile {
            serverUuids = [targetUuid]
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
                "local executor must receive the MANAGEMENT Role identity: " + "actual=${command.roleType}"
        assert command.sliceName == "zstack-management.slice" :
                "MANAGEMENT Role must use its configured systemd slice: " + "actual=${command.sliceName}"
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
                    "MANAGEMENT CPU PATCH must become the applied boundary: " + "expected=0-1 actual=${current.cpuSet}"
            assert current.memory == SizeUnit.MEGABYTE.toByte(256) :
                    "MANAGEMENT memory PATCH must be stored in bytes: " +
                            "expected=${SizeUnit.MEGABYTE.toByte(256)} " + "actual=${current.memory}"
            assert current.state == "Synced" :
                    "MANAGEMENT CPU and memory PATCH must apply locally: " + "expected=Synced actual=${current.state}"
        }
    }

    private void verifyManagedServiceUsageAndRestart() {
        String targetUuid = serverUuid
        def services = getPhysicalServerManagedServices {
            delegate.serverUuid = targetUuid
        }.services
        def managementNode = services.find {
            it.roleType == "MANAGEMENT" && it.serviceName == "management-node"
        }
        def prometheus = services.find {
            it.roleType == "MANAGEMENT" && it.serviceName == "prometheus"
        }
        assert managementNode?.state == "RUNNING" :
                "service inventory must expose the management node process: " + "actual=${managementNode}"
        assert managementNode.cpuSet == "0-1" && managementNode.memoryLimit == SizeUnit.MEGABYTE.toByte(256) :
                "service inventory must report the current Role boundary: " +
                        "actualCpuSet=${managementNode.cpuSet} " + "actualMemory=${managementNode.memoryLimit}"
        assert prometheus?.restartable :
                "manifest restartability must be visible to callers: " + "service=prometheus actual=${prometheus}"

        restartPhysicalServerManagedServices {
            delegate.serverUuid = targetUuid
            roleType = "MANAGEMENT"
            serviceNames = ["prometheus"]
        }
        List<ResourceConsumerHandle> restarted = executor.lastTestRestartHandles
        assert restarted*.serviceName == ["prometheus"] :
                "targeted restart must select only prometheus: " +
                        "expected=[prometheus] actual=${restarted*.serviceName}"
        assert restarted[0].value == "prometheus.service" :
                "restart must use the stable systemd unit from the manifest: " + "actual=${restarted[0].value}"

        restartPhysicalServerManagedServices {
            delegate.serverUuid = targetUuid
            roleType = "MANAGEMENT"
            serviceNames = ["vector"]
        }
        assert executor.lastTestRestartHandles*.serviceName == ["vector"] :
                "every service declared by the Role manifest must be addressable without an auxiliary flag"

        RestartPhysicalServerManagedServicesAction denied = new RestartPhysicalServerManagedServicesAction(
                        sessionId: adminSession(),
                        serverUuid: targetUuid, roleType: "MANAGEMENT", serviceNames: ["management-node"])
        def deniedResult = denied.call()
        assert deniedResult.error?.details?.contains("not a restartable systemd unit") :
                "a non-restartable core service must never be restarted by API: " + "actual=${deniedResult.error}"
    }

    private PhysicalServerResourceAssignmentInventory assignment() {
        List<PhysicalServerResourceAssignmentInventory> rows = queryPhysicalServerResourceAssignment {
            conditions = ["serverUuid=${serverUuid}", "roleType=MANAGEMENT"]
        } as List<PhysicalServerResourceAssignmentInventory>
        assert rows.size() == 1 :
                "one PhysicalServer may have only one MANAGEMENT Assignment: " +
                        "serverUuid=${serverUuid} actual=${rows.size()}"
        return rows[0]
    }

    private void verifyDisabledRelease(ManagementNodeResourceAssignmentFactory receiver) {
        AtomicReference<ResourceControlCommand> released = new AtomicReference<>()
        env.message(ReleaseManagementNodeResourceControlMsg) { msg, bus ->
            receiver.handleMessage(msg)
            released.set(msg.command)
        }
        PhysicalServerResourceAssignmentGlobalConfig.ENABLED.updateValue("false")
        retryInSecs {
            assert released.get() != null : "disabling resource assignment must send Release to the owning MN"
        }
        assert released.get().sliceName == "zstack-management.slice" :
                "Release must carry the sender's slice without consulting remote Profile: ${released.get().sliceName}"
        assert released.get().handles*.serviceName.contains("management-node") :
                "Release must carry sender-resolved services: ${released.get().handles*.serviceName}"
        assert assignment().state == "Unsynced" :
                "disabled MANAGEMENT must retain its Assignment as Unsynced, actual=${assignment().state}"
    }

    private static PhysicalServerCpuTopology topology() {
        PhysicalServerNumaNode node = new PhysicalServerNumaNode()
        node.nodeId = "0"
        node.onlineCpus = ["0", "1", "2", "3", "4", "5", "6", "7"]
        node.coreGroups = [["0", "4"], ["1", "5"], ["2", "6"], ["3", "7"]]
        return PhysicalServerCpuTopology.from(["0": node])
    }
}
