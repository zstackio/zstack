package org.zstack.test.integration.physicalserver

import org.springframework.http.HttpEntity
import org.zstack.core.Platform
import org.zstack.core.componentloader.PluginRegistry
import org.zstack.core.db.SQL
import org.zstack.header.core.Completion
import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.host.HostNUMANode
import org.zstack.header.host.HostVO
import org.zstack.header.physicalserver.PhysicalServerManager
import org.zstack.header.physicalserver.PhysicalServerIdentitySpec
import org.zstack.header.physicalserver.PhysicalServerCpuTopology
import org.zstack.header.physicalserver.ManagedServiceResourceUsage
import org.zstack.header.physicalserver.PhysicalServerResourceApplicationMode
import org.zstack.header.physicalserver.PhysicalServerResourceConsumerState
import org.zstack.header.physicalserver.PhysicalServerResourceControlAdapter
import org.zstack.header.physicalserver.PhysicalServerResourceIsolationMode
import org.zstack.header.physicalserver.ResourceControlCommand
import org.zstack.header.physicalserver.ResourceControlResponse
import org.zstack.header.physicalserver.ResourceControlResult
import org.zstack.kvm.KVMConstant
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KvmPhysicalServerAdapter
import org.zstack.portal.managementnode.LocalCpuTopologyCollector
import org.zstack.portal.managementnode.LocalResourceControlExecutor
import org.zstack.portal.managementnode.ManagementNodePhysicalServerAdapter
import org.zstack.physicalserver.PhysicalServerResourceAssignmentGlobalConfig
import org.zstack.sdk.HostInventory
import org.zstack.sdk.PhysicalServerResourceAssignmentInventory
import org.zstack.sdk.DeleteHostAction
import org.zstack.sdk.RefreshPhysicalServerResourceAssignmentsAction
import org.zstack.sdk.SystemTagInventory
import org.zstack.sdk.UpdatePhysicalServerResourceAssignmentAction
import org.zstack.test.integration.kvm.host.HostEnv
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SpringSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class PhysicalServerResourceAssignmentCase extends SubCase {
    static SpringSpec springSpec = PhysicalServerTest.springSpec

    EnvSpec env
    PhysicalServerManager physicalServerManager
    HostInventory host
    HostInventory forceDeleteHost
    String physicalServerUuid
    String forceDeletePhysicalServerUuid
    volatile boolean failResourceControl
    volatile boolean mismatchResourceControl
    AtomicInteger resourceControlCalls = new AtomicInteger()
    AtomicInteger capacityRefreshCalls = new AtomicInteger()
    AtomicReference<List<String>> restartedServices = new AtomicReference<>()
    AtomicReference<KvmPhysicalServerAdapter.ResourceControlAgentCommand> lastResourceControlCommand =
            new AtomicReference<>()
    AtomicReference<List<String>> inspectedServices = new AtomicReference<>()
    ManagementNodePhysicalServerAdapter managementAdapter
    LocalCpuTopologyCollector localTopology
    LocalResourceControlExecutor localExecutor
    List<PhysicalServerResourceControlAdapter> dynamicResourceControlAdapters = []
    String originalResourceAssignmentEnabled

    @Override
    void setup() {
        useSpring(springSpec)
    }

    @Override
    void environment() {
        env = HostEnv.twoHostEnv()
    }

    @Override
    void clean() {
        try {
            if (managementAdapter != null) {
                managementAdapter.setTestSerialNumber(null)
            }
            if (localTopology != null) {
                localTopology.clearTestTopology()
            }
            PhysicalServerTest.cleanupPhysicalServerRecords()
            env.delete()
        } finally {
            try {
                localExecutor?.setTestMode(false)
                bean(PluginRegistry.class).getExtensionList(
                        PhysicalServerResourceControlAdapter.class).removeAll(
                        dynamicResourceControlAdapters)
                dynamicResourceControlAdapters.clear()
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
            physicalServerManager = bean(PhysicalServerManager.class)
            host = env.inventoryByName("kvm1") as HostInventory
            forceDeleteHost = env.inventoryByName("kvm2") as HostInventory
            host = associateHostThroughSerialTag(
                    host, "physical-server-sdk-case")
            physicalServerUuid = host.serverUuid
            verifyIdentityResolution()
            verifyDuplicateHostSerialIsRejected()
            installCpuTopologySimulator()
            installResourceControlSimulator()

            verifyHeaderOnlyDynamicSharedRole()
            physicalServerManager.ensureResourceAssignment(
                    physicalServerUuid, "COMPUTE")
            refreshPhysicalServerResourceAssignments {
                serverUuid = physicalServerUuid
            }
            verifyKvmManifestAndManagedServiceApi()
            verifyInvalidUpdateDoesNotChangeAssignment()
            verifyUnsyncedAndRecovery()
            verifyConcurrentSparseUpdates()
            verifyExclusiveAndSharedCoexistence()
            verifySharedRolesAndAuxiliaryServiceOwnership()
            verifyGlobalSwitchGatesEnforcement()
            verifyHostCascadeCleanup()
        }
    }

    private HostInventory associateHostThroughSerialTag(
            HostInventory target, String serialNumber) {
        setHostSerialTag(target, serialNumber)
        bean(KvmPhysicalServerAdapter.class).associate(
                org.zstack.header.host.HostInventory.valueOf(
                        dbFindByUuid(target.uuid, HostVO.class)))
        AtomicReference<HostInventory> associated = new AtomicReference<>()
        retryInSecs {
            List<HostInventory> hosts = queryHost {
                conditions = ["uuid=${target.uuid}"]
            } as List<HostInventory>
            assert hosts.size() == 1 && hosts[0].serverUuid != null :
                    "Host serial association must persist serverUuid: " +
                            "hostUuid=${target.uuid} actual=${hosts*.serverUuid}"
            associated.set(hosts[0])
        }
        return associated.get()
    }

    private void setHostSerialTag(
            HostInventory target, String serialNumber) {
        List<SystemTagInventory> tags = querySystemTag {
            conditions = [
                    "resourceUuid=${target.uuid}".toString(),
                    "tag~=systemSerialNumber::%"
            ]
        } as List<SystemTagInventory>
        if (tags.isEmpty()) {
            createSystemTag {
                resourceUuid = target.uuid
                resourceType = "HostVO"
                tag = "systemSerialNumber::${serialNumber}"
            }
        } else {
            updateSystemTag {
                uuid = tags[0].uuid
                tag = "systemSerialNumber::${serialNumber}"
            }
        }
    }

    private void verifyIdentityResolution() {
        Map<String, String> normalized = physicalServerManager.resolveIdentities([
                new PhysicalServerIdentitySpec(
                        "  PHYSICAL-SERVER-SDK-CASE ", host.zoneUuid),
                new PhysicalServerIdentitySpec(
                        "physical-server-sdk-case", null)
        ])
        assert normalized == [
                "physical-server-sdk-case": physicalServerUuid
        ] :
                "equivalent serial reports must resolve to one PhysicalServer: " +
                        "expected=${physicalServerUuid} actual=${normalized}"

        Map<String, String> reverse =
                physicalServerManager.findSerialNumbersByServerUuids([
                        physicalServerUuid, Platform.getUuid()
                ])
        assert reverse == [
                (physicalServerUuid): "physical-server-sdk-case"
        ] :
                "bulk reverse identity lookup must ignore unknown UUIDs: " +
                        "expected=${physicalServerUuid}:physical-server-sdk-case " +
                        "actual=${reverse}"

        String conflictingZone = Platform.getUuid()
        assert physicalServerManager.resolveIdentities([
                new PhysicalServerIdentitySpec(
                        "physical-server-sdk-case", conflictingZone)
        ]).isEmpty() :
                "an existing PhysicalServer must never migrate across zones by observation: " +
                        "serverUuid=${physicalServerUuid} conflictingZone=${conflictingZone}"

        String ambiguousSerial = "ambiguous-physical-server-case"
        assert physicalServerManager.resolveIdentities([
                new PhysicalServerIdentitySpec(ambiguousSerial, host.zoneUuid),
                new PhysicalServerIdentitySpec(ambiguousSerial, conflictingZone)
        ]).isEmpty() :
                "one observation batch with conflicting zones must be rejected: " +
                        "serial=${ambiguousSerial}"
        assert (queryPhysicalServer {
            conditions = ["serialNumber=${ambiguousSerial}"]
        }).isEmpty() :
                "ambiguous identity input must not create a PhysicalServer row: " +
                        "serial=${ambiguousSerial}"
    }

    private void verifyDuplicateHostSerialIsRejected() {
        setHostSerialTag(forceDeleteHost, "physical-server-sdk-case")
        bean(KvmPhysicalServerAdapter.class).associate(
                org.zstack.header.host.HostInventory.valueOf(
                        dbFindByUuid(forceDeleteHost.uuid, HostVO.class)))
        List<HostInventory> hosts = queryHost {
            conditions = ["uuid=${forceDeleteHost.uuid}"]
        } as List<HostInventory>
        assert hosts.size() == 1 && hosts[0].serverUuid == null :
                "a PhysicalServer may be composed with only one live Host: " +
                        "serverUuid=${physicalServerUuid} secondHost=${hosts*.serverUuid}"
    }

    private void installCpuTopologySimulator() {
        env.simulator(KVMConstant.KVM_HOST_NUMA_PATH) {
            return validTopologyResponse()
        }
        env.afterSimulator(KVMConstant.KVM_HOST_CAPACITY_PATH) {
            response, HttpEntity<String> ignored ->
                capacityRefreshCalls.incrementAndGet()
                return response
        }
    }

    private KVMAgentCommands.GetHostNUMATopologyResponse validTopologyResponse() {
        HostNUMANode node = new HostNUMANode()
        node.nodeID = "0"
        node.cpus = ["0", "1", "2", "3", "4", "5", "6", "7"]
        node.onlineCpus = node.cpus
        node.coreGroups = [
                ["0", "4"],
                ["1", "5"],
                ["2", "6"],
                ["3", "7"]
        ]
        node.distance = ["10"]
        node.free = SizeUnit.GIGABYTE.toByte(8)
        node.size = SizeUnit.GIGABYTE.toByte(8)
        KVMAgentCommands.GetHostNUMATopologyResponse response =
                new KVMAgentCommands.GetHostNUMATopologyResponse()
        response.topology = ["0": node]
        return response
    }

    private void installResourceControlSimulator() {
        env.simulator(KvmPhysicalServerAdapter.APPLY_RESOURCE_CONTROL_PATH) {
            HttpEntity<String> entity -> resourceControlResponse(entity)
        }
        env.simulator(KvmPhysicalServerAdapter.GET_MANAGED_SERVICE_USAGE_PATH) {
            HttpEntity<String> entity ->
                KvmPhysicalServerAdapter.ManagedServiceAgentCommand command =
                        JSONObjectUtil.toObject(
                                entity.body,
                                KvmPhysicalServerAdapter.ManagedServiceAgentCommand.class)
                inspectedServices.set(command.handles.collect { it.serviceName })
                KvmPhysicalServerAdapter.ManagedServiceUsageAgentResponse response =
                        new KvmPhysicalServerAdapter.ManagedServiceUsageAgentResponse()
                response.services = command.handles.collect {
                    managedService(
                            "COMPUTE", it.serviceName, it.restartable,
                            it.serviceName == "node-exporter")
                }
                return response
        }
        env.simulator(KvmPhysicalServerAdapter.RESTART_MANAGED_SERVICES_PATH) {
            HttpEntity<String> entity ->
                KvmPhysicalServerAdapter.ManagedServiceAgentCommand command =
                        JSONObjectUtil.toObject(
                                entity.body,
                                KvmPhysicalServerAdapter.ManagedServiceAgentCommand.class)
                restartedServices.set(command.handles.collect { it.serviceName })
                return new KVMAgentCommands.AgentResponse()
        }
    }

    private void verifyKvmManifestAndManagedServiceApi() {
        retryInSecs {
            PhysicalServerResourceAssignmentInventory current = assignment()
            assert current.state == "Synced" :
                    "COMPUTE assignment must reconcile before service inspection: " +
                            "expected=Synced actual=${current.state}"
            assert lastResourceControlCommand.get() != null :
                    "KVM Apply must carry Role Manifest handles: actual=no command"
        }

        KvmPhysicalServerAdapter.ResourceControlAgentCommand applied =
                lastResourceControlCommand.get()
        assert applied.roleType == "COMPUTE" :
                "KVM command must preserve Role identity: " +
                        "expected=COMPUTE actual=${applied.roleType}"
        assert applied.sliceName == "zstack-compute.slice" :
                "KVM command must use the manifest Role slice: " +
                        "expected=zstack-compute.slice actual=${applied.sliceName}"
        assert applied.handles*.serviceName.containsAll([
                "kvmagent", "virtlogd", "node-exporter"
        ]) :
                "KVM command must be generated from core and auxiliary manifest services: " +
                        "actual=${applied.handles*.serviceName}"
        assert applied.handles.every {
            it.consumerKey == "host-agent:${host.uuid}"
        } :
                "every KVM Handle must be scoped to the associated Host consumer: " +
                        "expected=host-agent:${host.uuid} " +
                        "actual=${applied.handles*.consumerKey}"

        def services = getPhysicalServerManagedServices {
            serverUuid = physicalServerUuid
        }.services
        assert inspectedServices.get().containsAll([
                "kvmagent", "node-exporter"
        ]) :
                "managed-service query must send manifest handles to KVM Agent: " +
                        "actual=${inspectedServices.get()}"
        assert services.find {
            it.roleType == "COMPUTE" && it.serviceName == "node-exporter"
        }?.restartable :
                "managed-service inventory must expose manifest restartability: " +
                        "service=node-exporter actual=${services}"
        assert services.find {
            it.roleType == "COMPUTE" && it.serviceName == "node-exporter"
        }?.restartRequired :
                "managed-service inventory must expose a pending cgroup placement without persisting Handle state: " +
                        "service=node-exporter expected=true actual=${services}"

        refreshPhysicalServerResourceAssignments {
            serverUuid = physicalServerUuid
            roleType = "COMPUTE"
            serviceNames = ["node-exporter"]
        }
        assert restartedServices.get() == ["node-exporter"] :
                "targeted restart must send only the selected service: " +
                        "expected=[node-exporter] actual=${restartedServices.get()}"

        RefreshPhysicalServerResourceAssignmentsAction roleWithoutServices =
                new RefreshPhysicalServerResourceAssignmentsAction(
                        sessionId: adminSession(),
                        serverUuid: physicalServerUuid,
                        roleType: "COMPUTE")
        def roleWithoutServicesResult = roleWithoutServices.call()
        assert roleWithoutServicesResult.error?.details?.contains(
                "SERVICE_NAMES_REQUIRED") :
                "roleType without serviceNames must not change refresh semantics: " +
                        "expected=SERVICE_NAMES_REQUIRED " +
                        "actual=${roleWithoutServicesResult.error}"

        RefreshPhysicalServerResourceAssignmentsAction duplicateServices =
                new RefreshPhysicalServerResourceAssignmentsAction(
                        sessionId: adminSession(),
                        serverUuid: physicalServerUuid,
                        roleType: "COMPUTE",
                        serviceNames: ["node-exporter", "node-exporter"])
        def duplicateServicesResult = duplicateServices.call()
        assert duplicateServicesResult.error?.details?.contains(
                "SERVICE_NAME_SET_INVALID") :
                "duplicate serviceNames must be rejected before restart: " +
                        "expected=SERVICE_NAME_SET_INVALID " +
                        "actual=${duplicateServicesResult.error}"
    }

    private void verifyHeaderOnlyDynamicSharedRole() {
        String imageStoreRoleType = "IMAGE_STORE"
        AtomicBoolean associated = new AtomicBoolean(true)
        PhysicalServerResourceControlAdapter imageStore = [
                getRoleType: { imageStoreRoleType },
                getIsolationMode: {
                    PhysicalServerResourceIsolationMode.SHARED
                },
                getApplicationMode: {
                    PhysicalServerResourceApplicationMode.RESOURCE_HANDLES
                },
                getTopologyRoleType: { "COMPUTE" },
                getAssociatedServerUuids: {
                    associated.get()
                            ? [physicalServerUuid] as Set
                            : Collections.emptySet()
                },
                getState: { String ignored ->
                    associated.get()
                            ? PhysicalServerResourceConsumerState.AVAILABLE
                            : PhysicalServerResourceConsumerState.MISSING
                },
                getStates: { Collection<String> serverUuids ->
                    serverUuids.collectEntries {
                        [(it): associated.get()
                                ? PhysicalServerResourceConsumerState.AVAILABLE
                                : PhysicalServerResourceConsumerState.MISSING]
                    }
                },
                collectTopology: { String serverUuid, def completion ->
                    computeResourceControlAdapter().collectTopology(
                            serverUuid, completion)
                },
                apply: { String ignoredServer, String ignoredConsumer,
                         ResourceControlCommand command, def completion ->
                    completion.success(resourceHandleResponse(command))
                },
                collectManagedServiceUsage: {
                    String ignoredServer, boolean ignoredAuxiliary,
                    def completion ->
                        completion.success([
                                managedService(
                                        imageStoreRoleType, "image-store-agent", true)
                        ])
                },
                restartManagedServices: {
                    String ignoredServer, boolean ignoredAuxiliary,
                    Collection<String> serviceNames, def completion ->
                        restartedServices.set(new ArrayList<>(serviceNames))
                        completion.success()
                }
        ] as PhysicalServerResourceControlAdapter
        bean(PluginRegistry.class).defineDynamicExtension(
                PhysicalServerResourceControlAdapter.class, imageStore)
        dynamicResourceControlAdapters.add(imageStore)

        physicalServerManager.ensureResourceAssignment(
                physicalServerUuid, imageStoreRoleType)
        PhysicalServerResourceAssignmentInventory created = assignment(imageStoreRoleType)
        assert created.cpuSet == ""
        assert created.memory == null
        assert created.state == "Unsynced"

        def services = getPhysicalServerManagedServices {
            serverUuid = physicalServerUuid
        }.services
        def imageStoreService = services.find {
            it.roleType == imageStoreRoleType && it.serviceName == "image-store-agent"
        }
        assert imageStoreService != null
        assert imageStoreService.restartable
        assert imageStoreService.cpuSet == "0-7"

        refreshPhysicalServerResourceAssignments {
            serverUuid = physicalServerUuid
            roleType = imageStoreRoleType
            serviceNames = ["image-store-agent"]
        }
        assert restartedServices.get() == ["image-store-agent"]

        refreshPhysicalServerResourceAssignments {
            serverUuid = physicalServerUuid
        }
        retryInSecs {
            PhysicalServerResourceAssignmentInventory current =
                    assignment(imageStoreRoleType)
            assert current.cpuSet == "0-7"
            assert current.state == "Synced"
        }
        associated.set(false)
        int capacityCallsBeforeRelease = capacityRefreshCalls.get()
        verifyReleaseDeletesAssignment(imageStoreRoleType, null)
        assert capacityRefreshCalls.get() == capacityCallsBeforeRelease :
                "releasing a shared Role must not trigger Host capacity recomputation"
    }

    private void verifyInvalidUpdateDoesNotChangeAssignment() {
        PhysicalServerResourceAssignmentInventory before = assignment()

        UpdatePhysicalServerResourceAssignmentAction invalidCpu =
                new UpdatePhysicalServerResourceAssignmentAction(
                        sessionId: adminSession(),
                        serverUuid: physicalServerUuid,
                        roleType: "COMPUTE",
                        cpuSet: "999999"
                )
        UpdatePhysicalServerResourceAssignmentAction.Result cpuResult =
                invalidCpu.call()
        assert cpuResult.error != null
        assert cpuResult.error.details.contains("CPU_SET_INVALID")

        UpdatePhysicalServerResourceAssignmentAction invalidMemory =
                new UpdatePhysicalServerResourceAssignmentAction(
                        sessionId: adminSession(),
                        serverUuid: physicalServerUuid,
                        roleType: "COMPUTE",
                        memory: -1L
                )
        UpdatePhysicalServerResourceAssignmentAction.Result memoryResult =
                invalidMemory.call()
        assert memoryResult.error != null
        assert memoryResult.error.details.contains("MEMORY_INVALID")

        UpdatePhysicalServerResourceAssignmentAction unalignedMemory =
                new UpdatePhysicalServerResourceAssignmentAction(
                        sessionId: adminSession(),
                        serverUuid: physicalServerUuid,
                        roleType: "COMPUTE",
                        memory: 1L
                )
        def unalignedMemoryResult = unalignedMemory.call()
        assert unalignedMemoryResult.error?.details?.contains(
                "MEMORY_INVALID") :
                "memory must use the platform byte unit with 1 MiB alignment: " +
                        "actual=${unalignedMemoryResult.error}"

        UpdatePhysicalServerResourceAssignmentAction empty =
                new UpdatePhysicalServerResourceAssignmentAction(
                        sessionId: adminSession(),
                        serverUuid: physicalServerUuid,
                        roleType: "COMPUTE"
                )
        UpdatePhysicalServerResourceAssignmentAction.Result emptyResult =
                empty.call()
        assert emptyResult.error != null
        assert emptyResult.error.details.contains(
                "RESOURCE_ASSIGNMENT_UPDATE_EMPTY")

        UpdatePhysicalServerResourceAssignmentAction unsupportedRole =
                new UpdatePhysicalServerResourceAssignmentAction(
                        sessionId: adminSession(),
                        serverUuid: physicalServerUuid,
                        roleType: "UNKNOWN_ROLE",
                        cpuSet: "0"
                )
        assert unsupportedRole.call().error?.details?.contains(
                "ROLE_TYPE_NOT_SUPPORTED") :
                "an unregistered Role must not create an Assignment implicitly"

        UpdatePhysicalServerResourceAssignmentAction missingAssignment =
                new UpdatePhysicalServerResourceAssignmentAction(
                        sessionId: adminSession(),
                        serverUuid: physicalServerUuid,
                        roleType: "IMAGE_STORE",
                        cpuSet: "0"
                )
        assert missingAssignment.call().error?.details?.contains(
                "RESOURCE_ASSIGNMENT_NOT_FOUND") :
                "a registered Adapter without an owner relation must not recreate deleted configuration"

        RefreshPhysicalServerResourceAssignmentsAction invalidServiceName =
                new RefreshPhysicalServerResourceAssignmentsAction(
                        sessionId: adminSession(),
                        serverUuid: physicalServerUuid,
                        roleType: "COMPUTE",
                        serviceNames: ["bad service name"])
        assert invalidServiceName.call().error?.details?.contains(
                "SERVICE_NAME_SET_INVALID") :
                "service restart accepts stable service identities only"

        RefreshPhysicalServerResourceAssignmentsAction tooManyServices =
                new RefreshPhysicalServerResourceAssignmentsAction(
                        sessionId: adminSession(),
                        serverUuid: physicalServerUuid,
                        roleType: "COMPUTE",
                        serviceNames: (0..64).collect {
                            "service-${it}".toString()
                        })
        assert tooManyServices.call().error?.details?.contains(
                "SERVICE_NAME_SET_INVALID") :
                "one restart request must remain bounded to 64 services"

        RefreshPhysicalServerResourceAssignmentsAction undefinedService =
                new RefreshPhysicalServerResourceAssignmentsAction(
                        sessionId: adminSession(),
                        serverUuid: physicalServerUuid,
                        roleType: "COMPUTE",
                        serviceNames: ["undefined-service"])
        assert undefinedService.call().error?.details?.contains(
                "not defined by role") :
                "a syntactically valid but unconfigured service must be rejected by the Role manifest"

        try {
            env.simulator(KVMConstant.KVM_HOST_NUMA_PATH) {
                HostNUMANode node = new HostNUMANode()
                node.nodeID = "0"
                node.cpus = ["0", "1"]
                node.onlineCpus = node.cpus
                node.coreGroups = [["0"]]
                KVMAgentCommands.GetHostNUMATopologyResponse response =
                        new KVMAgentCommands.GetHostNUMATopologyResponse()
                response.topology = ["0": node]
                return response
            }
            UpdatePhysicalServerResourceAssignmentAction invalidTopology =
                    new UpdatePhysicalServerResourceAssignmentAction(
                            sessionId: adminSession(),
                            serverUuid: physicalServerUuid,
                            roleType: "COMPUTE",
                            cpuSet: "0")
            assert invalidTopology.call().error?.details?.contains(
                    "CPU_TOPOLOGY_FACT_INVALID") :
                    "Cloud must reject an incomplete Host topology before changing the ledger"
        } finally {
            env.simulator(KVMConstant.KVM_HOST_NUMA_PATH) {
                return validTopologyResponse()
            }
        }

        PhysicalServerResourceAssignmentInventory after = assignment()
        assert after.cpuSet == before.cpuSet
        assert after.memory == before.memory

        refreshPhysicalServerResourceAssignments {
            serverUuid = physicalServerUuid
        }
        retryInSecs {
            PhysicalServerResourceAssignmentInventory current = assignment()
            assert current.cpuSet == before.cpuSet
            assert current.memory == before.memory
            assert current.state == "Synced" :
                    "restoring valid topology must let the unchanged Assignment converge"
        }
    }

    private void verifyUnsyncedAndRecovery() {
        failResourceControl = true
        int callsBeforeFailure = resourceControlCalls.get()
        PhysicalServerResourceAssignmentInventory updated =
                updatePhysicalServerResourceAssignment {
            serverUuid = physicalServerUuid
            roleType = "COMPUTE"
            cpuSet = "0-1"
            memory = SizeUnit.MEGABYTE.toByte(64)
        }
        assert updated.cpuSet == "0-1"
        assert updated.memory == SizeUnit.MEGABYTE.toByte(64)
        assert updated.state == "Unsynced"

        refreshPhysicalServerResourceAssignments {
            serverUuid = physicalServerUuid
        }
        retryInSecs {
            assert resourceControlCalls.get() > callsBeforeFailure
            assert assignment().state == "Unsynced"
        }

        failResourceControl = false
        refreshPhysicalServerResourceAssignments {
            serverUuid = physicalServerUuid
        }
        retryInSecs {
            PhysicalServerResourceAssignmentInventory current = assignment()
            assert current.cpuSet == "0-1"
            assert current.memory == SizeUnit.MEGABYTE.toByte(64)
            assert current.state == "Synced"
        }

        mismatchResourceControl = true
        updatePhysicalServerResourceAssignment {
            serverUuid = physicalServerUuid
            roleType = "COMPUTE"
            memory = SizeUnit.MEGABYTE.toByte(80)
        }
        retryInSecs {
            assert assignment().state == "Unsynced" :
                    "an HTTP-success response with incomplete service coverage must not be trusted"
        }
        mismatchResourceControl = false
        refreshPhysicalServerResourceAssignments {
            serverUuid = physicalServerUuid
        }
        retryInSecs {
            assert assignment().state == "Synced" :
                    "the same Assignment must recover after complete coverage is observed"
        }
    }

    private void verifyConcurrentSparseUpdates() {
        CountDownLatch start = new CountDownLatch(1)
        CountDownLatch finished = new CountDownLatch(2)
        List<Throwable> failures = Collections.synchronizedList([])
        String sessionUuid = adminSession()

        Thread.start {
            try {
                start.await()
                UpdatePhysicalServerResourceAssignmentAction.Result result =
                        new UpdatePhysicalServerResourceAssignmentAction(
                                sessionId: sessionUuid,
                                serverUuid: physicalServerUuid,
                                roleType: "COMPUTE",
                                cpuSet: "0-2"
                        ).call()
                assert result.error == null : result.error
            } catch (Throwable error) {
                failures.add(error)
            } finally {
                finished.countDown()
            }
        }
        Thread.start {
            try {
                start.await()
                UpdatePhysicalServerResourceAssignmentAction.Result result =
                        new UpdatePhysicalServerResourceAssignmentAction(
                                sessionId: sessionUuid,
                                serverUuid: physicalServerUuid,
                                roleType: "COMPUTE",
                                memory: SizeUnit.MEGABYTE.toByte(96)
                        ).call()
                assert result.error == null : result.error
            } catch (Throwable error) {
                failures.add(error)
            } finally {
                finished.countDown()
            }
        }

        start.countDown()
        assert finished.await(30, TimeUnit.SECONDS)
        assert failures.isEmpty() : failures
        retryInSecs {
            PhysicalServerResourceAssignmentInventory current = assignment()
            assert current.cpuSet == "0-2" :
                    "memory PATCH must preserve the concurrent CPU PATCH"
            assert current.memory == SizeUnit.MEGABYTE.toByte(96) :
                    "CPU PATCH must preserve the concurrent memory PATCH"
            assert current.state == "Synced"
        }

        updatePhysicalServerResourceAssignment {
            serverUuid = physicalServerUuid
            roleType = "COMPUTE"
            cpuSet = "0-1"
            memory = SizeUnit.MEGABYTE.toByte(64)
        }
        retryInSecs {
            PhysicalServerResourceAssignmentInventory current = assignment()
            assert current.cpuSet == "0-1"
            assert current.memory == SizeUnit.MEGABYTE.toByte(64)
            assert current.state == "Synced"
        }
    }

    private void verifyGlobalSwitchGatesEnforcement() {
        PhysicalServerResourceAssignmentGlobalConfig.ENABLED.updateValue("false")
        try {
            refreshPhysicalServerResourceAssignments {
                serverUuid = physicalServerUuid
            }
            retryInSecs {
                assert assignment().state == "Unsynced" :
                        "disabled resource assignment must retain the ledger without applying it"
            }

            UpdatePhysicalServerResourceAssignmentAction.Result result =
                    new UpdatePhysicalServerResourceAssignmentAction(
                            sessionId: adminSession(),
                            serverUuid: physicalServerUuid,
                            roleType: "COMPUTE",
                            cpuSet: "0-1").call()
            assert result.error?.details?.contains(
                    "RESOURCE_ASSIGNMENT_DISABLED") :
                    "disabled resource assignment must reject enforcement updates: " +
                            "actual=${result.error}"
        } finally {
            PhysicalServerResourceAssignmentGlobalConfig.ENABLED.updateValue("true")
        }

        refreshPhysicalServerResourceAssignments {
            serverUuid = physicalServerUuid
        }
        retryInSecs {
            assert assignment().state == "Synced" :
                    "re-enabling must reconcile the existing ledger"
        }
    }

    private void verifyExclusiveAndSharedCoexistence() {
        String storageRoleType = "TEST_STORAGE"
        int capacityCallsBeforeExclusive = capacityRefreshCalls.get()
        PhysicalServerResourceControlAdapter exclusiveStorage = [
                getRoleType: { storageRoleType },
                getIsolationMode: {
                    PhysicalServerResourceIsolationMode.EXCLUSIVE
                },
                getApplicationMode: {
                    PhysicalServerResourceApplicationMode.PROVIDER_MANAGED
                },
                getTopologyRoleType: { "COMPUTE" },
                getAssociatedServerUuids: {
                    [physicalServerUuid] as Set
                },
                getState: { String ignored ->
                    PhysicalServerResourceConsumerState.AVAILABLE
                },
                getStates: { Collection<String> serverUuids ->
                    serverUuids.collectEntries {
                        [(it): PhysicalServerResourceConsumerState.AVAILABLE]
                    }
                },
                collectTopology: { String serverUuid, def completion ->
                    computeResourceControlAdapter().collectTopology(
                            serverUuid, completion)
                },
                apply: { String ignoredServer, String ignoredConsumer,
                         ResourceControlCommand command, def completion ->
                    ResourceControlResponse response =
                            new ResourceControlResponse()
                    boolean release = command.operation == "RELEASE"
                    response.state = release ? "DISABLED" : "READY"
                    response.cpuSet = release ? "" : command.cpuSet
                    response.memory = release ? null : command.memory
                    completion.success(response)
                },
                collectManagedServiceUsage: {
                    String ignoredServer, boolean ignoredAuxiliary,
                    def completion ->
                        completion.success([
                                managedService(
                                        storageRoleType,
                                        "test-storage", false)
                        ])
                },
                restartManagedServices: {
                    String ignoredServer, boolean ignoredAuxiliary,
                    Collection<String> ignoredServices, def completion ->
                        completion.success()
                }
        ] as PhysicalServerResourceControlAdapter
        bean(PluginRegistry.class).defineDynamicExtension(
                PhysicalServerResourceControlAdapter.class, exclusiveStorage)
        dynamicResourceControlAdapters.add(exclusiveStorage)
        physicalServerManager.ensureResourceAssignment(
                physicalServerUuid, storageRoleType)

        def service = getPhysicalServerManagedServices {
            serverUuid = physicalServerUuid
        }.services.find {
            it.roleType == storageRoleType
        }
        assert service != null :
                "provider-managed Role must expose its managed service: " +
                        "roleType=${storageRoleType} actual=no service"
        assert !service.restartable :
                "provider-managed service restart must remain owned by Provider: " +
                        "roleType=${storageRoleType} actualRestartable=${service.restartable}"

        RefreshPhysicalServerResourceAssignmentsAction providerRestart =
                new RefreshPhysicalServerResourceAssignmentsAction(
                        sessionId: adminSession(),
                        serverUuid: physicalServerUuid,
                        roleType: storageRoleType,
                        serviceNames: ["test-storage"])
        def providerRestartResult = providerRestart.call()
        assert providerRestartResult.error?.details?.contains(
                "SERVICE_RESTART_NOT_SUPPORTED") :
                "provider-managed Role must reject Cloud service restart: " +
                        "expected=SERVICE_RESTART_NOT_SUPPORTED " +
                        "actual=${providerRestartResult.error}"

        UpdatePhysicalServerResourceAssignmentAction splitCore =
                new UpdatePhysicalServerResourceAssignmentAction(
                        sessionId: adminSession(),
                        serverUuid: physicalServerUuid,
                        roleType: storageRoleType,
                        cpuSet: "2")
        assert splitCore.call().error?.details?.contains(
                "CPU_SIBLING_SPLIT") :
                "an exclusive Role must reserve every sibling of a physical core"

        UpdatePhysicalServerResourceAssignmentAction cpuZero =
                new UpdatePhysicalServerResourceAssignmentAction(
                        sessionId: adminSession(),
                        serverUuid: physicalServerUuid,
                        roleType: storageRoleType,
                        cpuSet: "0,4")
        assert cpuZero.call().error?.details?.contains(
                "CPU_ZERO_RESERVED") :
                "CPU0 must remain outside every exclusive service boundary"

        updatePhysicalServerResourceAssignment {
            serverUuid = physicalServerUuid
            roleType = storageRoleType
            cpuSet = "2-3,6-7"
            memory = SizeUnit.MEGABYTE.toByte(128)
        }
        retryInSecs {
            PhysicalServerResourceAssignmentInventory current =
                    assignment(storageRoleType)
            assert current.cpuSet == "2-3,6-7"
            assert current.memory == SizeUnit.MEGABYTE.toByte(128)
            assert current.state == "Synced"
        }
        retryInSecs {
            assert capacityRefreshCalls.get() > capacityCallsBeforeExclusive :
                    "a newly Synced exclusive boundary must trigger Host capacity recomputation"
        }

        PhysicalServerResourceAssignmentInventory before = assignment()
        UpdatePhysicalServerResourceAssignmentAction overlap =
                new UpdatePhysicalServerResourceAssignmentAction(
                        sessionId: adminSession(),
                        serverUuid: physicalServerUuid,
                        roleType: "COMPUTE",
                        cpuSet: "0-3"
                )
        UpdatePhysicalServerResourceAssignmentAction.Result overlapResult =
                overlap.call()
        assert overlapResult.error != null
        assert overlapResult.error.details.contains("CPU_SET_CONFLICT")
        assert assignment().cpuSet == before.cpuSet

        updatePhysicalServerResourceAssignment {
            serverUuid = physicalServerUuid
            roleType = "COMPUTE"
            cpuSet = "0-1,4-5"
        }
        retryInSecs {
            PhysicalServerResourceAssignmentInventory current = assignment()
            assert current.cpuSet == "0-1,4-5"
            assert current.memory == SizeUnit.MEGABYTE.toByte(64)
            assert current.state == "Synced"
        }

        int capacityCallsBeforeRelease = capacityRefreshCalls.get()
        verifyReleaseDeletesAssignment(storageRoleType, null)
        retryInSecs {
            assert capacityRefreshCalls.get() > capacityCallsBeforeRelease :
                    "releasing an exclusive boundary must restore Host capacity immediately"
        }
    }

    private void verifySharedRolesAndAuxiliaryServiceOwnership() {
        managementAdapter = bean(ManagementNodePhysicalServerAdapter.class)
        localTopology = bean(LocalCpuTopologyCollector.class)
        localExecutor = bean(LocalResourceControlExecutor.class)
        managementAdapter.setTestSerialNumber("physical-server-sdk-case")
        SQL.New("update ManagementNodeVO m set m.serverUuid = null " +
                "where m.uuid = :uuid")
                .param("uuid", Platform.getManagementServerId())
                .execute()
        managementAdapter.refreshAssociations()
        localTopology.setTestTopology(PhysicalServerCpuTopology.from([
                "0": new org.zstack.header.physicalserver.PhysicalServerNumaNode(
                        nodeId: "0",
                        onlineCpus: ["0", "1", "2", "3", "4", "5", "6", "7"],
                        coreGroups: [
                                ["0", "4"], ["1", "5"],
                                ["2", "6"], ["3", "7"]
                        ])
        ]))
        localExecutor.setTestMode(true)
        managementAdapter.associateLocalNode(Platform.getManagementServerId())

        refreshPhysicalServerResourceAssignments {
            serverUuid = physicalServerUuid
        }
        retryInSecs {
            assert assignment(physicalServerUuid, "MANAGEMENT").state ==
                    "Synced" :
                    "MANAGEMENT and COMPUTE shared Roles must coexist on one PhysicalServer"
        }
        def services = getPhysicalServerManagedServices {
            serverUuid = physicalServerUuid
        }.services
        assert services.find {
            it.roleType == "MANAGEMENT" &&
                    it.serviceName == "node-exporter"
        } != null :
                "auxiliary services must be owned by MANAGEMENT when that Role exists"
        assert services.find {
            it.roleType == "COMPUTE" &&
                    it.serviceName == "node-exporter"
        } == null :
                "COMPUTE must not duplicate an auxiliary service already owned by MANAGEMENT"
        assert !inspectedServices.get().contains("node-exporter") :
                "KVM inspection command must omit auxiliary handles when MANAGEMENT owns them: " +
                        "actual=${inspectedServices.get()}"
    }

    private void verifyHostCascadeCleanup() {
        failResourceControl = true
        DeleteHostAction.Result blocked = new DeleteHostAction(
                sessionId: adminSession(),
                uuid: host.uuid,
                deleteMode: "Permissive").call()
        assert blocked.error != null :
                "normal Host deletion must stop when resource release fails: " +
                        "hostUuid=${host.uuid} actual=success"
        assert queryHost {
            conditions = ["uuid=${host.uuid}"]
        }.size() == 1 :
                "blocked Host deletion must preserve the Host relation: " +
                        "hostUuid=${host.uuid} actual=missing"
        assert assignment().state == "Unsynced" :
                "failed cascade release must preserve an Unsynced ledger for retry: " +
                        "expected=Unsynced actual=${assignment().state}"

        failResourceControl = false
        DeleteHostAction.Result deleted = new DeleteHostAction(
                sessionId: adminSession(),
                uuid: host.uuid,
                deleteMode: "Permissive").call()
        assert deleted.error == null :
                "normal Host deletion must continue after release succeeds: " +
                        "hostUuid=${host.uuid} actual=${deleted.error}"
        retryInSecs {
            assert (queryHost {
                conditions = ["uuid=${host.uuid}"]
            }).isEmpty() :
                    "successful Host cascade must delete the Host: " +
                            "hostUuid=${host.uuid} actual=still present"
            assert assignments(physicalServerUuid, "COMPUTE").isEmpty() :
                    "successful Host cascade must delete the COMPUTE Assignment: " +
                            "serverUuid=${physicalServerUuid} actual=${assignments(physicalServerUuid, 'COMPUTE')}"
        }

        forceDeleteHost = associateHostThroughSerialTag(
                forceDeleteHost, "physical-server-force-delete-case")
        forceDeletePhysicalServerUuid = forceDeleteHost.serverUuid
        physicalServerManager.ensureResourceAssignment(
                forceDeletePhysicalServerUuid, "COMPUTE")
        refreshPhysicalServerResourceAssignments {
            serverUuid = forceDeletePhysicalServerUuid
        }
        retryInSecs {
            PhysicalServerResourceAssignmentInventory current = assignment(
                    forceDeletePhysicalServerUuid, "COMPUTE")
            assert current.state == "Synced" :
                    "force-delete scenario requires a synced Assignment first: " +
                            "expected=Synced actual=${current.state}"
        }

        failResourceControl = true
        DeleteHostAction.Result forceDeleted = new DeleteHostAction(
                sessionId: adminSession(),
                uuid: forceDeleteHost.uuid,
                deleteMode: "Enforcing").call()
        assert forceDeleted.error == null :
                "enforcing Host deletion must not be blocked by an unreachable executor: " +
                        "hostUuid=${forceDeleteHost.uuid} actual=${forceDeleted.error}"
        retryInSecs {
            assert (queryHost {
                conditions = ["uuid=${forceDeleteHost.uuid}"]
            }).isEmpty() :
                    "enforcing cascade must delete the Host: " +
                            "hostUuid=${forceDeleteHost.uuid} actual=still present"
            assert assignments(
                    forceDeletePhysicalServerUuid, "COMPUTE").isEmpty() :
                    "Role removal must not retain configuration after enforcing deletion: " +
                            "serverUuid=${forceDeletePhysicalServerUuid} " +
                            "actual=${assignments(forceDeletePhysicalServerUuid, 'COMPUTE')}"
        }
        failResourceControl = false
    }

    private void verifyReleaseDeletesAssignment(
            String roleType, String consumerUuid) {
        CountDownLatch released = new CountDownLatch(1)
        AtomicReference<ErrorCode> failure = new AtomicReference<>()
        physicalServerManager.releaseResourceAssignment(
                physicalServerUuid, roleType, consumerUuid,
                new Completion(null) {
            @Override
            void success() {
                released.countDown()
            }

            @Override
            void fail(ErrorCode errorCode) {
                failure.set(errorCode)
                released.countDown()
            }
        })

        assert released.await(30, TimeUnit.SECONDS)
        assert failure.get() == null
        List<PhysicalServerResourceAssignmentInventory> assignments =
                queryPhysicalServerResourceAssignment {
            conditions = [
                    "serverUuid=${physicalServerUuid}",
                    "roleType=${roleType}"
            ]
        }
        assert assignments.isEmpty()
    }

    private PhysicalServerResourceAssignmentInventory assignment() {
        return assignment("COMPUTE")
    }

    private PhysicalServerResourceAssignmentInventory assignment(
            String roleType) {
        return assignment(physicalServerUuid, roleType)
    }

    private PhysicalServerResourceAssignmentInventory assignment(
            String serverUuid, String roleType) {
        List<PhysicalServerResourceAssignmentInventory> current =
                assignments(serverUuid, roleType)
        assert current.size() == 1 :
                "exactly one Assignment must exist for serverUuid and roleType: " +
                        "serverUuid=${serverUuid} roleType=${roleType} " +
                        "actualCount=${current.size()}"
        return current[0]
    }

    private List<PhysicalServerResourceAssignmentInventory> assignments(
            String serverUuid, String roleType) {
        return queryPhysicalServerResourceAssignment {
            conditions = [
                    "serverUuid=${serverUuid}",
                    "roleType=${roleType}"
            ]
        } as List<PhysicalServerResourceAssignmentInventory>
    }

    private PhysicalServerResourceControlAdapter computeResourceControlAdapter() {
        return bean(PluginRegistry.class).getExtensionList(
                PhysicalServerResourceControlAdapter.class).find {
            it.roleType == "COMPUTE"
        }
    }

    private KvmPhysicalServerAdapter.ResourceControlAgentResponse resourceControlResponse(
            HttpEntity<String> entity) {
        KvmPhysicalServerAdapter.ResourceControlAgentCommand command =
                JSONObjectUtil.toObject(
                        entity.body,
                        KvmPhysicalServerAdapter.ResourceControlAgentCommand.class)
        lastResourceControlCommand.set(command)
        KvmPhysicalServerAdapter.ResourceControlAgentResponse response =
                new KvmPhysicalServerAdapter.ResourceControlAgentResponse()
        resourceControlCalls.incrementAndGet()
        if (failResourceControl) {
            response.error = "simulated resource control failure"
            return response
        }

        boolean release = command.operation == "RELEASE"
        response.cpuSet = release ? "" : command.cpuSet
        response.memory = command.memory == null
                ? null : release ? 0L : command.memory
        response.expectedServiceCount = mismatchResourceControl ? 2 : 1
        response.coveredServiceCount = 1
        ResourceControlResult result = new ResourceControlResult()
        result.state = release ? "DISABLED" : "READY"
        result.cpuSet = response.cpuSet
        result.memory = response.memory
        response.results = [result]
        return response
    }

    private ResourceControlResponse resourceHandleResponse(
            ResourceControlCommand command) {
        boolean release = command.operation == "RELEASE"
        ResourceControlResponse response = new ResourceControlResponse()
        response.cpuSet = release ? "" : command.cpuSet
        response.memory = command.memory == null
                ? null : release ? 0L : command.memory
        response.expectedServiceCount = 1
        response.coveredServiceCount = 1
        ResourceControlResult result = new ResourceControlResult()
        result.state = release ? "DISABLED" : "READY"
        result.cpuSet = response.cpuSet
        result.memory = response.memory
        response.results = [result]
        return response
    }

    private ManagedServiceResourceUsage managedService(
            String roleType, String serviceName, boolean restartable,
            boolean restartRequired = false) {
        ManagedServiceResourceUsage usage = new ManagedServiceResourceUsage()
        usage.roleType = roleType
        usage.serviceName = serviceName
        usage.restartable = restartable
        usage.restartRequired = restartRequired
        usage.state = "RUNNING"
        usage.cpuSet = "0-7"
        usage.cpuTime = 1000L
        usage.memory = SizeUnit.MEGABYTE.toByte(96)
        usage.memoryLimit = 0L
        return usage
    }
}
