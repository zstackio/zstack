package org.zstack.test.integration.physicalserver

import org.springframework.http.HttpEntity
import org.zstack.core.Platform
import org.zstack.core.componentloader.PluginRegistry
import org.zstack.core.db.SQL
import org.zstack.header.core.Completion
import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.host.HostNUMANode
import org.zstack.header.host.HostVO
import org.zstack.header.physicalserver.ManagedServiceResourceUsage
import org.zstack.header.physicalserver.PhysicalServerCpuSet
import org.zstack.header.physicalserver.PhysicalServerCpuTopology
import org.zstack.header.physicalserver.PhysicalServerManager
import org.zstack.header.physicalserver.PhysicalServerResourceAssignmentController
import org.zstack.header.physicalserver.PhysicalServerResourceBoundary
import org.zstack.header.physicalserver.PhysicalServerResourceIsolationMode
import org.zstack.header.physicalserver.PhysicalServerResourceUsageObserver
import org.zstack.header.physicalserver.PhysicalServerRoleAssociationProvider
import org.zstack.header.physicalserver.PhysicalServerRoleType
import org.zstack.header.physicalserver.ResourceConsumerHandle
import org.zstack.header.physicalserver.ResourceControlCommand
import org.zstack.kvm.KVMConstant
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KvmPhysicalServerAdapter
import org.zstack.portal.managementnode.LocalCpuTopologyCollector
import org.zstack.portal.managementnode.LocalResourceControlExecutor
import org.zstack.portal.managementnode.ManagementNodePhysicalServerAdapter
import org.zstack.physicalserver.PhysicalServerResourceAssignmentGlobalConfig
import org.zstack.sdk.ApiException
import org.zstack.sdk.DeleteHostAction
import org.zstack.sdk.HostInventory
import org.zstack.sdk.PhysicalServerResourceAssignmentInventory
import org.zstack.sdk.RefreshPhysicalServerResourceAssignmentsFromProfileAction
import org.zstack.sdk.RestartPhysicalServerManagedServicesAction
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
    AtomicReference<List<String>> restartedServices = new AtomicReference<>()
    AtomicReference<KvmPhysicalServerAdapter.ApplyResourceControlAgentCommand> lastResourceControlCommand =
            new AtomicReference<>()
    AtomicReference<List<String>> inspectedServices = new AtomicReference<>()
    ManagementNodePhysicalServerAdapter managementAdapter
    LocalCpuTopologyCollector localTopology
    LocalResourceControlExecutor localExecutor
    List<PhysicalServerResourceAssignmentController> dynamicControllers = []
    List<PhysicalServerResourceUsageObserver> dynamicUsageObservers = []
    List<PhysicalServerRoleAssociationProvider> dynamicAssociationProviders = []
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
                localExecutor?.disableTestMode()
                bean(PluginRegistry.class).getExtensionList(
                        PhysicalServerResourceAssignmentController.class).removeAll(dynamicControllers)
                bean(PluginRegistry.class).getExtensionList(
                        PhysicalServerResourceUsageObserver.class).removeAll(dynamicUsageObservers)
                bean(PluginRegistry.class).getExtensionList(
                        PhysicalServerRoleAssociationProvider.class).removeAll(dynamicAssociationProviders)
                dynamicControllers.clear()
                dynamicUsageObservers.clear()
                dynamicAssociationProviders.clear()
            } finally {
                if (originalResourceAssignmentEnabled != null) {
                    PhysicalServerResourceAssignmentGlobalConfig.ENABLED.updateValue(originalResourceAssignmentEnabled)
                }
            }
        }
    }

    @Override
    void test() {
        env.create {
            originalResourceAssignmentEnabled = PhysicalServerResourceAssignmentGlobalConfig.ENABLED.value()
            PhysicalServerResourceAssignmentGlobalConfig.ENABLED.updateValue("true")
            physicalServerManager = bean(PhysicalServerManager.class)
            host = env.inventoryByName("kvm1") as HostInventory
            forceDeleteHost = env.inventoryByName("kvm2") as HostInventory
            host = associateHostThroughSerialTag(host, "physical-server-sdk-case")
            physicalServerUuid = host.serverUuid
            verifyIdentityResolution()
            verifyDuplicateHostSerialIsRejected()
            installCpuTopologySimulator()
            installResourceControlSimulator()

            verifyHeaderOnlyDynamicSharedRole()
            refreshPhysicalServerResourceAssignmentsFromProfile {
                serverUuids = [physicalServerUuid]
            }
            verifyKvmManifestAndManagedServiceApi()
            verifyScopedBatchAndGlobalRefresh()
            verifyInvalidUpdateDoesNotChangeAssignment()
            verifyUnsyncedAndRecovery()
            verifyConcurrentSparseUpdates()
            verifyExclusiveAndSharedCoexistence()
            verifySharedHandleOwnership()
            verifyGlobalSwitchGatesEnforcement()
            verifyHostCascadeCleanup()
        }
    }

    private HostInventory associateHostThroughSerialTag(HostInventory target, String serialNumber) {
        setHostSerialTag(target, serialNumber)
        bean(KvmPhysicalServerAdapter.class).associate(
                org.zstack.header.host.HostInventory.valueOf(dbFindByUuid(target.uuid, HostVO.class)))
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

    private void setHostSerialTag(HostInventory target, String serialNumber) {
        List<SystemTagInventory> tags = querySystemTag {
            conditions = ["resourceUuid=${target.uuid}".toString(), "tag~=systemSerialNumber::%"]
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
        Map<String, String> normalized = physicalServerManager.resolveBySerialNumbers([
                "  PHYSICAL-SERVER-SDK-CASE ", "physical-server-sdk-case"])
        assert normalized == ["physical-server-sdk-case": physicalServerUuid] :
                "equivalent serial reports must resolve to one PhysicalServer: " +
                        "expected=${physicalServerUuid} actual=${normalized}"

    }

    private void verifyDuplicateHostSerialIsRejected() {
        setHostSerialTag(forceDeleteHost, "physical-server-sdk-case")
        bean(KvmPhysicalServerAdapter.class).associate(
                org.zstack.header.host.HostInventory.valueOf(dbFindByUuid(forceDeleteHost.uuid, HostVO.class)))
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
    }

    private KVMAgentCommands.GetHostNUMATopologyResponse validTopologyResponse() {
        HostNUMANode node = new HostNUMANode()
        node.nodeID = "0"
        node.cpus = (0..15).collect { it.toString() }
        node.onlineCpus = node.cpus
        node.coreGroups = [
                ["0", "4"], ["1", "5"], ["2", "6"], ["3", "7"], ["8", "12"], ["9", "13"], ["10", "14"], ["11", "15"]]
        node.distance = ["10"]
        node.free = SizeUnit.GIGABYTE.toByte(8)
        node.size = SizeUnit.GIGABYTE.toByte(8)
        KVMAgentCommands.GetHostNUMATopologyResponse response = new KVMAgentCommands.GetHostNUMATopologyResponse()
        response.topology = ["0": node]
        return response
    }

    private void installResourceControlSimulator() {
        env.simulator(KvmPhysicalServerAdapter.APPLY_RESOURCE_CONTROL_PATH) {
            HttpEntity<String> entity -> applyResourceControlResponse(entity)
        }
        env.simulator(KvmPhysicalServerAdapter.RELEASE_RESOURCE_CONTROL_PATH) {
            HttpEntity<String> entity -> releaseResourceControlResponse(entity)
        }
        env.simulator(KvmPhysicalServerAdapter.GET_MANAGED_SERVICE_USAGE_PATH) {
            HttpEntity<String> entity ->
                KvmPhysicalServerAdapter.ManagedServiceAgentCommand command =
                        JSONObjectUtil.toObject(entity.body, KvmPhysicalServerAdapter.ManagedServiceAgentCommand.class)
                inspectedServices.set(command.handles.collect { it.serviceName })
                KvmPhysicalServerAdapter.ManagedServiceUsageAgentResponse response =
                        new KvmPhysicalServerAdapter.ManagedServiceUsageAgentResponse()
                response.services = command.handles.collect {
                    ManagedServiceResourceUsage usage = managedService("COMPUTE", it.serviceName)
                    usage.restartable = it.restartable
                    usage.restartRequired = it.serviceName == "node-exporter"
                    return usage
                }
                return response
        }
        env.simulator(KvmPhysicalServerAdapter.RESTART_MANAGED_SERVICES_PATH) {
            HttpEntity<String> entity ->
                KvmPhysicalServerAdapter.ManagedServiceAgentCommand command =
                        JSONObjectUtil.toObject(entity.body, KvmPhysicalServerAdapter.ManagedServiceAgentCommand.class)
                restartedServices.set(command.handles.collect { it.serviceName })
                return new KVMAgentCommands.AgentResponse()
        }
    }

    private void verifyKvmManifestAndManagedServiceApi() {
        retryInSecs {
            PhysicalServerResourceAssignmentInventory current = assignment()
            assert current.state == "Synced" :
                    "COMPUTE assignment must apply before service inspection: " +
                            "expected=Synced actual=${current.state}"
            assert lastResourceControlCommand.get() != null :
                    "KVM Apply must carry Role Manifest handles: actual=no command"
        }

        KvmPhysicalServerAdapter.ApplyResourceControlAgentCommand applied = lastResourceControlCommand.get()
        assert applied.roleType == "COMPUTE" :
                "KVM command must preserve Role identity: " + "expected=COMPUTE actual=${applied.roleType}"
        assert applied.sliceName == "zstack-compute.slice" :
                "KVM command must use the manifest Role slice: " +
                        "expected=zstack-compute.slice actual=${applied.sliceName}"
        assert applied.isolationMode == "SHARED" :
                "KVM command must carry the Profile isolation mode: " +
                        "expected=SHARED actual=${applied.isolationMode}"
        assert applied.handles*.serviceName.containsAll(["kvmagent", "virtlogd", "node-exporter"]) :
                "KVM command must be generated from core and auxiliary manifest services: " +
                        "actual=${applied.handles*.serviceName}"
        def services = getPhysicalServerManagedServices {
            serverUuid = physicalServerUuid
        }.services
        assert inspectedServices.get().containsAll(["kvmagent", "node-exporter"]) :
                "managed-service query must send manifest handles to KVM Agent: " + "actual=${inspectedServices.get()}"
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

        restartPhysicalServerManagedServices {
            serverUuid = physicalServerUuid
            roleType = "COMPUTE"
            serviceNames = ["node-exporter"]
        }
        assert restartedServices.get() == ["node-exporter"] :
                "targeted restart must send only the selected service: " +
                        "expected=[node-exporter] actual=${restartedServices.get()}"

        RestartPhysicalServerManagedServicesAction roleWithoutServices =
                new RestartPhysicalServerManagedServicesAction(
                        sessionId: adminSession(), serverUuid: physicalServerUuid, roleType: "COMPUTE")
        expect(ApiException.class) {
            roleWithoutServices.call()
        }

        RestartPhysicalServerManagedServicesAction duplicateServices =
                new RestartPhysicalServerManagedServicesAction(
                        sessionId: adminSession(),
                        serverUuid: physicalServerUuid,
                        roleType: "COMPUTE", serviceNames: ["node-exporter", "node-exporter"])
        def duplicateServicesResult = duplicateServices.call()
        assert duplicateServicesResult.error?.details?.contains("Service names must be non-empty and unique") :
                "duplicate serviceNames must be rejected before restart: " + "actual=${duplicateServicesResult.error}"
    }

    private void verifyScopedBatchAndGlobalRefresh() {
        forceDeleteHost = associateHostThroughSerialTag(forceDeleteHost, "physical-server-force-delete-case")
        forceDeletePhysicalServerUuid = forceDeleteHost.serverUuid
        retryInSecs {
            String currentState = assignment(forceDeletePhysicalServerUuid, "COMPUTE").state
            assert currentState == "Synced" :
                    "the second PhysicalServer must be ready before global Refresh: " +
                            "expected=Synced actual=${currentState}"
        }

        ApiException emptyScope = null
        try {
            new RefreshPhysicalServerResourceAssignmentsFromProfileAction(
                    sessionId: adminSession(), serverUuids: []).call()
        } catch (ApiException error) {
            emptyScope = error
        }
        assert emptyScope?.message?.contains("field[serverUuids] cannot be an empty list") :
                "an explicit empty serverUuids list must be rejected instead of " +
                        "silently selecting every Assignment: actual=${emptyScope.message}"

        int callsBeforeScopedRefresh = resourceControlCalls.get()
        refreshPhysicalServerResourceAssignmentsFromProfile {
            serverUuids = [physicalServerUuid, forceDeletePhysicalServerUuid]
        }
        retryInSecs {
            assert resourceControlCalls.get() >= callsBeforeScopedRefresh + 2 :
                    "scoped Refresh must accept and process multiple PhysicalServers: " +
                            "expectedCalls>=${callsBeforeScopedRefresh + 2} " +
                            "actualCalls=${resourceControlCalls.get()}"
        }

        int callsBeforeGlobalRefresh = resourceControlCalls.get()
        refreshPhysicalServerResourceAssignmentsFromProfile { }
        retryInSecs {
            PhysicalServerResourceAssignmentInventory first = assignment(physicalServerUuid, "COMPUTE")
            PhysicalServerResourceAssignmentInventory second = assignment(forceDeletePhysicalServerUuid, "COMPUTE")
            assert first.state == "Synced" && second.state == "Synced" &&
                    resourceControlCalls.get() >= callsBeforeGlobalRefresh + 2 :
                    "omitting serverUuids must enqueue every Assignment owner: " +
                            "firstState=${first.state} secondState=${second.state} " +
                            "expectedCalls>=${callsBeforeGlobalRefresh + 2} " +
                            "actualCalls=${resourceControlCalls.get()}"
        }
    }

    private void verifyHeaderOnlyDynamicSharedRole() {
        String imageStoreRoleType = "IMAGE_STORE"
        PhysicalServerRoleType imageStoreRole = registeredRoleType(imageStoreRoleType)
        long memoryLimit = SizeUnit.MEGABYTE.toByte(256)
        AtomicBoolean associated = new AtomicBoolean(true)
        AtomicInteger topologyCalls = new AtomicInteger()
        AtomicInteger applyCalls = new AtomicInteger()
        AtomicReference<Integer> defaultCpuCount = new AtomicReference<>()
        AtomicReference<Long> observedMemory = new AtomicReference<>()
        AtomicReference<ResourceControlCommand> applied = new AtomicReference<>()
        PhysicalServerResourceAssignmentController imageStore = [
                getRoleType: { imageStoreRole },
                getIsolationMode: {
                    PhysicalServerResourceIsolationMode.SHARED
                },
                getDefaultCpuCount: { defaultCpuCount.get() },
                getResourceConsumers: { String ignored ->
                    [restartableSystemdHandle("image-store-agent")]
                },
                collectResourceAssignment: {
                    String ignoredServer, def completion ->
                        completion.success(new PhysicalServerResourceBoundary(
                                cpuSet: "0-7", memory: observedMemory.get()))
                },
                collectTopology: { String ignoredServer, def completion ->
                    topologyCalls.incrementAndGet()
                    computeResourceControlAdapter().collectTopology(ignoredServer, completion)
                },
                apply: { String ignoredServer, ResourceControlCommand command, def completion ->
                    applyCalls.incrementAndGet()
                    applied.set(command)
                    observedMemory.set(command.memory)
                    completion.success(true)
                },
                release: { String ignoredServer, ResourceControlCommand command, def completion ->
                    applyCalls.incrementAndGet()
                    applied.set(command)
                    observedMemory.set(null)
                    completion.success(true)
                },
                restartManagedServices: {
                    String ignoredServer,
                    Collection<ResourceConsumerHandle> consumers, def completion ->
                        restartedServices.set(consumers*.serviceName)
                        completion.success()
                }
        ] as PhysicalServerResourceAssignmentController
        PhysicalServerResourceUsageObserver usage = [
                getRoleType: { imageStoreRole },
                collectManagedServiceUsage: {
                    String ignoredServer, def completion ->
                        completion.success([restartableManagedService(imageStoreRoleType, "image-store-agent")])
                }
        ] as PhysicalServerResourceUsageObserver
        PhysicalServerRoleAssociationProvider associations = [
                getRoleType: { imageStoreRole },
                discoverAssociations: { Collection<String> scope ->
                    if (!associated.get()) {
                        return Collections.emptySet()
                    }
                    if (scope == null || scope.isEmpty() || scope.contains(physicalServerUuid)) {
                        return [physicalServerUuid] as Set
                    }
                    return Collections.emptySet()
                }
        ] as PhysicalServerRoleAssociationProvider
        registerDynamicRole(imageStore, usage, associations)

        assert assignments(physicalServerUuid, imageStoreRoleType).isEmpty()
        def servicesBeforeDiscovery = getPhysicalServerManagedServices {
            serverUuid = physicalServerUuid
        }.services
        assert !servicesBeforeDiscovery.any { it.roleType == imageStoreRoleType } :
                "managed-service GET must not discover relations or create Assignments"

        physicalServerManager.associationChanged(physicalServerUuid)
        refreshPhysicalServerResourceAssignmentsFromProfile {
            serverUuids = [physicalServerUuid]
        }
        retryInSecs {
            assert assignment(imageStoreRoleType).state == "Synced"
        }
        def services = getPhysicalServerManagedServices {
            serverUuid = physicalServerUuid
        }.services
        def imageStoreService = services.find {
            it.roleType == imageStoreRoleType && it.serviceName == "image-store-agent"
        }
        assert imageStoreService != null
        assert imageStoreService.restartable
        assert imageStoreService.cpuSet == "0-7"

        restartPhysicalServerManagedServices {
            serverUuid = physicalServerUuid
            roleType = imageStoreRoleType
            serviceNames = ["image-store-agent"]
        }
        assert restartedServices.get() == ["image-store-agent"]

        PhysicalServerResourceAssignmentInventory updated = updatePhysicalServerResourceAssignment {
            serverUuid = physicalServerUuid
            roleType = imageStoreRoleType
            memory = memoryLimit
        }
        assert !updated.cpuSet
        assert updated.memory == memoryLimit
        retryInSecs {
            assert assignment(imageStoreRoleType).state == "Synced"
            assert observedMemory.get() == memoryLimit
        }

        int callsBeforeMatchingRefresh = applyCalls.get()
        refreshPhysicalServerResourceAssignmentsFromProfile {
            serverUuids = [physicalServerUuid]
        }
        retryInSecs {
            PhysicalServerResourceAssignmentInventory current = assignment(imageStoreRoleType)
            assert !current.cpuSet :
                    "a memory-only Role must not synthesize a CPU boundary"
            assert current.memory == memoryLimit
            assert current.state == "Synced"
            assert applied.get() != null
            assert !applied.get().cpuSet
            assert applied.get().memory == memoryLimit
            assert topologyCalls.get() == 0 :
                    "a memory-only Role must not query CPU topology"
            assert applyCalls.get() > callsBeforeMatchingRefresh :
                    "Refresh must apply the current Profile even when observation already matches the desired boundary"
        }


        defaultCpuCount.set(4)
        refreshPhysicalServerResourceAssignmentsFromProfile {
            serverUuids = [physicalServerUuid]
        }
        retryInSecs {
            assert PhysicalServerCpuSet.count(assignment(imageStoreRoleType).cpuSet) == 4 :
                    "adding defaultCpuCount must allocate the requested CPU count"
        }

        String beforeExpansion = assignment(imageStoreRoleType).cpuSet
        defaultCpuCount.set(6)
        refreshPhysicalServerResourceAssignmentsFromProfile {
            serverUuids = [physicalServerUuid]
        }
        retryInSecs {
            String expanded = assignment(imageStoreRoleType).cpuSet
            assert PhysicalServerCpuSet.count(expanded) == 6
            assert PhysicalServerCpuSet.parse(expanded).containsAll(PhysicalServerCpuSet.parse(beforeExpansion)) :
                    "Profile Refresh must expand the existing CPU set: " + "before=${beforeExpansion} after=${expanded}"
        }

        updatePhysicalServerResourceAssignment {
            serverUuid = physicalServerUuid
            roleType = imageStoreRoleType
            cpuSet = "8-11"
        }
        retryInSecs {
            assert assignment(imageStoreRoleType).cpuSet == "8-11" :
                    "Update must preserve an explicit CPU set instead of applying the Profile count"
        }

        defaultCpuCount.set(null)
        refreshPhysicalServerResourceAssignmentsFromProfile {
            serverUuids = [physicalServerUuid]
        }
        retryInSecs {
            assert assignment(imageStoreRoleType).cpuSet == "8-11" :
                    "removing defaultCpuCount must leave the current Assignment unchanged"
        }
        associated.set(false)
        verifyReleaseDeletesAssignment(imageStoreRoleType)
    }

    private void verifyInvalidUpdateDoesNotChangeAssignment() {
        PhysicalServerResourceAssignmentInventory before = assignment()

        UpdatePhysicalServerResourceAssignmentAction invalidCpu =
                new UpdatePhysicalServerResourceAssignmentAction(
                        sessionId: adminSession(),
                        serverUuid: physicalServerUuid, roleType: "COMPUTE", cpuSet: "999999")
        UpdatePhysicalServerResourceAssignmentAction.Result cpuResult = invalidCpu.call()
        assert cpuResult.error != null
        assert cpuResult.error.details.contains("outside the online topology")

        UpdatePhysicalServerResourceAssignmentAction invalidMemory =
                new UpdatePhysicalServerResourceAssignmentAction(
                        sessionId: adminSession(), serverUuid: physicalServerUuid, roleType: "COMPUTE", memory: -1L)
        UpdatePhysicalServerResourceAssignmentAction.Result memoryResult = invalidMemory.call()
        assert memoryResult.error != null
        assert memoryResult.error.details.contains("positive multiple of 1 MiB")

        UpdatePhysicalServerResourceAssignmentAction unalignedMemory =
                new UpdatePhysicalServerResourceAssignmentAction(
                        sessionId: adminSession(), serverUuid: physicalServerUuid, roleType: "COMPUTE", memory: 1L)
        def unalignedMemoryResult = unalignedMemory.call()
        assert unalignedMemoryResult.error?.details?.contains("positive multiple of 1 MiB") :
                "memory must use the platform byte unit with 1 MiB alignment: " +
                        "actual=${unalignedMemoryResult.error}"

        UpdatePhysicalServerResourceAssignmentAction empty =
                new UpdatePhysicalServerResourceAssignmentAction(
                        sessionId: adminSession(), serverUuid: physicalServerUuid, roleType: "COMPUTE")
        UpdatePhysicalServerResourceAssignmentAction.Result emptyResult = empty.call()
        assert emptyResult.error != null
        assert emptyResult.error.details.contains("CpuSet or memory must be specified")

        UpdatePhysicalServerResourceAssignmentAction unsupportedRole =
                new UpdatePhysicalServerResourceAssignmentAction(
                        sessionId: adminSession(),
                        serverUuid: physicalServerUuid, roleType: "UNKNOWN_ROLE", cpuSet: "0")
        assert unsupportedRole.call().error != null :
                "an unregistered Role must be rejected"
        assert assignments(physicalServerUuid, "UNKNOWN_ROLE").isEmpty() :
                "an unregistered Role must not create an Assignment implicitly"

        UpdatePhysicalServerResourceAssignmentAction missingAssignment =
                new UpdatePhysicalServerResourceAssignmentAction(
                        sessionId: adminSession(), serverUuid: physicalServerUuid, roleType: "IMAGE_STORE", cpuSet: "0")
        assert missingAssignment.call().error?.details?.contains("does not exist on physical server") :
                "a registered Adapter without an owner relation must not recreate deleted configuration"

        RestartPhysicalServerManagedServicesAction invalidServiceName =
                new RestartPhysicalServerManagedServicesAction(
                        sessionId: adminSession(),
                        serverUuid: physicalServerUuid, roleType: "COMPUTE", serviceNames: ["bad service name"])
        assert invalidServiceName.call().error?.details?.contains("Service names must be non-empty and unique") :
                "service restart accepts stable service identities only"

        RestartPhysicalServerManagedServicesAction tooManyServices =
                new RestartPhysicalServerManagedServicesAction(
                        sessionId: adminSession(),
                        serverUuid: physicalServerUuid, roleType: "COMPUTE", serviceNames: (0..64).collect {
                            "service-${it}".toString()
                        })
        assert tooManyServices.call().error?.details?.contains("At most 64 services") :
                "one restart request must remain bounded to 64 services"

        RestartPhysicalServerManagedServicesAction undefinedService =
                new RestartPhysicalServerManagedServicesAction(
                        sessionId: adminSession(),
                        serverUuid: physicalServerUuid, roleType: "COMPUTE", serviceNames: ["undefined-service"])
        assert undefinedService.call().error?.details?.contains("are not managed by roleType") :
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
                            sessionId: adminSession(), serverUuid: physicalServerUuid, roleType: "COMPUTE", cpuSet: "0")
            assert invalidTopology.call().error?.details?.contains("Host returned invalid CPU topology") :
                    "Cloud must reject an incomplete Host topology before changing the ledger"
        } finally {
            env.simulator(KVMConstant.KVM_HOST_NUMA_PATH) {
                return validTopologyResponse()
            }
        }

        PhysicalServerResourceAssignmentInventory after = assignment()
        assert after.cpuSet == before.cpuSet
        assert after.memory == before.memory

        refreshPhysicalServerResourceAssignmentsFromProfile {
            serverUuids = [physicalServerUuid]
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
        PhysicalServerResourceAssignmentInventory updated = updatePhysicalServerResourceAssignment {
            serverUuid = physicalServerUuid
            roleType = "COMPUTE"
            cpuSet = "0-1"
            memory = SizeUnit.MEGABYTE.toByte(64)
        }
        assert updated.cpuSet == "0-1"
        assert updated.memory == SizeUnit.MEGABYTE.toByte(64)
        assert updated.state == "Unsynced"

        refreshPhysicalServerResourceAssignmentsFromProfile {
            serverUuids = [physicalServerUuid]
        }
        retryInSecs {
            assert resourceControlCalls.get() > callsBeforeFailure
            assert assignment().state == "Unsynced"
        }

        failResourceControl = false
        refreshPhysicalServerResourceAssignmentsFromProfile {
            serverUuids = [physicalServerUuid]
        }
        retryInSecs {
            PhysicalServerResourceAssignmentInventory current = assignment()
            assert PhysicalServerCpuSet.count(current.cpuSet) == 8
            assert PhysicalServerCpuSet.parse(current.cpuSet).containsAll([0, 1]) :
                    "Profile Refresh must expand the current CPU set to defaultCpuCount"
            assert current.memory == SizeUnit.MEGABYTE.toByte(64)
            assert current.state == "Synced"
        }

        updatePhysicalServerResourceAssignment {
            serverUuid = physicalServerUuid
            roleType = "COMPUTE"
            memory = SizeUnit.MEGABYTE.toByte(72)
        }
        retryInSecs {
            assert assignment().state == "Synced" :
                    "an Apply response with synced=true must promote the desired Assignment"
        }

        mismatchResourceControl = true
        updatePhysicalServerResourceAssignment {
            serverUuid = physicalServerUuid
            roleType = "COMPUTE"
            memory = SizeUnit.MEGABYTE.toByte(80)
        }
        retryInSecs {
            assert assignment().state == "Unsynced" :
                    "an HTTP-success response with synced=false must keep the Assignment Unsynced"
        }
        mismatchResourceControl = false
        refreshPhysicalServerResourceAssignmentsFromProfile {
            serverUuids = [physicalServerUuid]
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
                                serverUuid: physicalServerUuid, roleType: "COMPUTE", cpuSet: "0-2").call()
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
                                roleType: "COMPUTE", memory: SizeUnit.MEGABYTE.toByte(96)).call()
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
            refreshPhysicalServerResourceAssignmentsFromProfile {
                serverUuids = [physicalServerUuid]
            }
            retryInSecs {
                assert assignment().state == "Unsynced" :
                        "disabled resource assignment must retain the ledger without applying it"
            }

            UpdatePhysicalServerResourceAssignmentAction.Result result =
                    new UpdatePhysicalServerResourceAssignmentAction(
                            sessionId: adminSession(),
                            serverUuid: physicalServerUuid, roleType: "COMPUTE", cpuSet: "0-1").call()
            assert result.error?.details?.contains("Resource assignment is disabled") :
                    "disabled resource assignment must reject enforcement updates: " + "actual=${result.error}"
        } finally {
            PhysicalServerResourceAssignmentGlobalConfig.ENABLED.updateValue("true")
        }

        refreshPhysicalServerResourceAssignmentsFromProfile {
            serverUuids = [physicalServerUuid]
        }
        retryInSecs {
            assert assignment().state == "Synced" :
                    "re-enabling must apply the existing Assignment"
        }
    }

    private void verifyExclusiveAndSharedCoexistence() {
        String storageRoleType = "TEST_STORAGE"
        PhysicalServerRoleType storageRole = registeredRoleType(storageRoleType)
        AtomicBoolean storageAssociated = new AtomicBoolean(true)
        PhysicalServerResourceAssignmentController exclusiveStorage = [
                getRoleType: { storageRole },
                getIsolationMode: {
                    PhysicalServerResourceIsolationMode.EXCLUSIVE
                },
                getDefaultCpuCount: { 4 },
                getResourceConsumers: { String ignored ->
                    [restartableSystemdHandle("test-storage")]
                },
                collectResourceAssignment: {
                    String ignoredServer, def completion ->
                        completion.success(new PhysicalServerResourceBoundary(
                                cpuSet: "2-3,6-7", memory: SizeUnit.MEGABYTE.toByte(128)))
                },
                collectTopology: { String serverUuid, def completion ->
                    computeResourceControlAdapter().collectTopology(serverUuid, completion)
                },
                apply: { String ignoredServer, ResourceControlCommand command, def completion ->
                    completion.success(true)
                },
                release: { String ignoredServer, ResourceControlCommand command, def completion ->
                    completion.success(true)
                },
                restartManagedServices: {
                    String ignoredServer,
                    Collection<ResourceConsumerHandle> consumers, def completion ->
                        restartedServices.set(consumers*.serviceName)
                        completion.success()
                }
        ] as PhysicalServerResourceAssignmentController
        PhysicalServerResourceUsageObserver usage = [
                getRoleType: { storageRole },
                collectManagedServiceUsage: {
                    String ignoredServer, def completion ->
                        completion.success([restartableManagedService(storageRoleType, "test-storage")])
                }
        ] as PhysicalServerResourceUsageObserver
        PhysicalServerRoleAssociationProvider associations = [
                getRoleType: { storageRole },
                discoverAssociations: { Collection<String> scope ->
                    if (storageAssociated.get()
                            && (scope == null || scope.isEmpty() || scope.contains(physicalServerUuid))) {
                        return [physicalServerUuid] as Set
                    }
                    return Collections.emptySet()
                }
        ] as PhysicalServerRoleAssociationProvider
        registerDynamicRole(exclusiveStorage, usage, associations)
        physicalServerManager.associationChanged(physicalServerUuid)
        refreshPhysicalServerResourceAssignmentsFromProfile {
            serverUuids = [physicalServerUuid]
        }
        retryInSecs {
            assert assignments(physicalServerUuid, storageRoleType).size() == 1
        }
        updatePhysicalServerResourceAssignment {
            serverUuid = physicalServerUuid
            roleType = storageRoleType
            cpuSet = "2-3,6-7"
            memory = SizeUnit.MEGABYTE.toByte(128)
        }
        retryInSecs {
            PhysicalServerResourceAssignmentInventory current = assignment(storageRoleType)
            assert current.cpuSet == "2-3,6-7"
            assert current.memory == SizeUnit.MEGABYTE.toByte(128)
            assert current.state == "Synced"
        }

        def service = getPhysicalServerManagedServices {
            serverUuid = physicalServerUuid
        }.services.find {
            it.roleType == storageRoleType
        }
        assert service != null :
                "exclusive Role must expose its managed service: " + "roleType=${storageRoleType} actual=no service"
        assert service.restartable :
                "exclusive and shared Roles must use the same managed-service contract: " +
                        "roleType=${storageRoleType} actualRestartable=${service.restartable}"

        restartPhysicalServerManagedServices {
            serverUuid = physicalServerUuid
            roleType = storageRoleType
            serviceNames = ["test-storage"]
        }
        assert restartedServices.get() == ["test-storage"] :
                "exclusive and shared Roles must use the same restart path"

        UpdatePhysicalServerResourceAssignmentAction splitCore =
                new UpdatePhysicalServerResourceAssignmentAction(
                        sessionId: adminSession(),
                        serverUuid: physicalServerUuid, roleType: storageRoleType, cpuSet: "2")
        assert splitCore.call().error?.details?.contains("CPU set splits core group") :
                "an exclusive Role must reserve every sibling of a physical core"

        UpdatePhysicalServerResourceAssignmentAction cpuZero =
                new UpdatePhysicalServerResourceAssignmentAction(
                        sessionId: adminSession(),
                        serverUuid: physicalServerUuid, roleType: storageRoleType, cpuSet: "0,4")
        assert cpuZero.call().error?.details?.contains("CPU0 core group must remain shared") :
                "CPU0 must remain outside every exclusive service boundary"

        PhysicalServerResourceAssignmentInventory before = assignment()
        UpdatePhysicalServerResourceAssignmentAction overlap =
                new UpdatePhysicalServerResourceAssignmentAction(
                        sessionId: adminSession(), serverUuid: physicalServerUuid, roleType: "COMPUTE", cpuSet: "0-3")
        UpdatePhysicalServerResourceAssignmentAction.Result overlapResult = overlap.call()
        assert overlapResult.error != null
        assert overlapResult.error.details.contains("CPU set overlaps an exclusive role")
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

        storageAssociated.set(false)
        verifyReleaseDeletesAssignment(storageRoleType)
    }

    private void verifySharedHandleOwnership() {
        managementAdapter = bean(ManagementNodePhysicalServerAdapter.class)
        localTopology = bean(LocalCpuTopologyCollector.class)
        localExecutor = bean(LocalResourceControlExecutor.class)
        managementAdapter.setTestSerialNumber("physical-server-sdk-case")
        SQL.New("update ManagementNodeVO m set m.serverUuid = null " +
                "where m.uuid = :uuid").param("uuid", Platform.getManagementServerId()).execute()
        managementAdapter.discoverAssociations(Collections.emptySet())
        localTopology.setTestTopology(PhysicalServerCpuTopology.from([
                "0": new org.zstack.header.physicalserver.PhysicalServerNumaNode(
                        nodeId: "0",
                        onlineCpus: (0..15).collect { it.toString() },
                        coreGroups: [
                                ["0", "4"], ["1", "5"],
                                ["2", "6"], ["3", "7"], ["8", "12"], ["9", "13"], ["10", "14"], ["11", "15"]])]))
        localExecutor.enableTestMode()
        managementAdapter.associateLocalNode(Platform.getManagementServerId())

        refreshPhysicalServerResourceAssignmentsFromProfile {
            serverUuids = [physicalServerUuid]
        }
        retryInSecs {
            assert assignment(physicalServerUuid, "MANAGEMENT").state == "Synced" :
                    "MANAGEMENT and COMPUTE shared Roles must coexist on one PhysicalServer"
        }
        def services = getPhysicalServerManagedServices {
            serverUuid = physicalServerUuid
        }.services
        assert services.find {
            it.roleType == "MANAGEMENT" && it.serviceName == "node-exporter"
        } != null :
                "a shared service handle must have one deterministic Role owner"
        assert services.find {
            it.roleType == "COMPUTE" && it.serviceName == "node-exporter"
        } == null :
                "COMPUTE must not duplicate a handle already owned by MANAGEMENT"
    }

    private void verifyHostCascadeCleanup() {
        failResourceControl = true
        DeleteHostAction.Result blocked = new DeleteHostAction(
                sessionId: adminSession(), uuid: host.uuid, deleteMode: "Permissive").call()
        assert blocked.error != null :
                "normal Host deletion must stop when resource release fails: " + "hostUuid=${host.uuid} actual=success"
        assert queryHost {
            conditions = ["uuid=${host.uuid}"]
        }.size() == 1 :
                "blocked Host deletion must preserve the Host relation: " + "hostUuid=${host.uuid} actual=missing"
        assert assignment().state == "Unsynced" :
                "failed cascade release must preserve an Unsynced ledger for retry: " +
                        "expected=Unsynced actual=${assignment().state}"

        failResourceControl = false
        DeleteHostAction.Result deleted = new DeleteHostAction(
                sessionId: adminSession(), uuid: host.uuid, deleteMode: "Permissive").call()
        assert deleted.error == null :
                "normal Host deletion must continue after release succeeds: " +
                        "hostUuid=${host.uuid} actual=${deleted.error}"
        retryInSecs {
            assert (queryHost {
                conditions = ["uuid=${host.uuid}"]
            }).isEmpty() :
                    "successful Host cascade must delete the Host: " + "hostUuid=${host.uuid} actual=still present"
            assert assignments(physicalServerUuid, "COMPUTE").isEmpty() :
                    "successful Host cascade must delete the COMPUTE Assignment: " +
                            "serverUuid=${physicalServerUuid} actual=${assignments(physicalServerUuid, 'COMPUTE')}"
        }

        failResourceControl = true
        DeleteHostAction.Result forceDeleted = new DeleteHostAction(
                sessionId: adminSession(), uuid: forceDeleteHost.uuid, deleteMode: "Enforcing").call()
        assert forceDeleted.error == null :
                "enforcing Host deletion must not be blocked by an unreachable executor: " +
                        "hostUuid=${forceDeleteHost.uuid} actual=${forceDeleted.error}"
        retryInSecs {
            assert (queryHost {
                conditions = ["uuid=${forceDeleteHost.uuid}"]
            }).isEmpty() :
                    "enforcing cascade must delete the Host: " + "hostUuid=${forceDeleteHost.uuid} actual=still present"
            assert assignments(forceDeletePhysicalServerUuid, "COMPUTE").isEmpty() :
                    "Role removal must not retain configuration after enforcing deletion: " +
                            "serverUuid=${forceDeletePhysicalServerUuid} " +
                            "actual=${assignments(forceDeletePhysicalServerUuid, 'COMPUTE')}"
        }
        failResourceControl = false
    }

    private void verifyReleaseDeletesAssignment(String roleType) {
        CountDownLatch released = new CountDownLatch(1)
        AtomicReference<ErrorCode> failure = new AtomicReference<>()
        physicalServerManager.releaseResourceAssignment(physicalServerUuid, roleType, new Completion(null) {
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
        List<PhysicalServerResourceAssignmentInventory> assignments = queryPhysicalServerResourceAssignment {
            conditions = ["serverUuid=${physicalServerUuid}", "roleType=${roleType}"]
        }
        assert assignments.isEmpty()
    }

    private PhysicalServerResourceAssignmentInventory assignment() {
        return assignment("COMPUTE")
    }

    private PhysicalServerResourceAssignmentInventory assignment(String roleType) {
        return assignment(physicalServerUuid, roleType)
    }

    private PhysicalServerResourceAssignmentInventory assignment(String serverUuid, String roleType) {
        List<PhysicalServerResourceAssignmentInventory> current = assignments(serverUuid, roleType)
        assert current.size() == 1 :
                "exactly one Assignment must exist for serverUuid and roleType: " +
                        "serverUuid=${serverUuid} roleType=${roleType} " + "actualCount=${current.size()}"
        return current[0]
    }

    private List<PhysicalServerResourceAssignmentInventory> assignments(String serverUuid, String roleType) {
        return queryPhysicalServerResourceAssignment {
            conditions = ["serverUuid=${serverUuid}", "roleType=${roleType}"]
        } as List<PhysicalServerResourceAssignmentInventory>
    }

    private PhysicalServerResourceAssignmentController computeResourceControlAdapter() {
        return bean(PluginRegistry.class).getExtensionList(PhysicalServerResourceAssignmentController.class).find {
            it.roleType.toString() == "COMPUTE"
        }
    }

    private static PhysicalServerRoleType registeredRoleType(String typeName) {
        try {
            return PhysicalServerRoleType.valueOf(typeName)
        } catch (IllegalArgumentException ignored) {
            return new PhysicalServerRoleType(typeName)
        }
    }

    private void registerDynamicRole(
            PhysicalServerResourceAssignmentController controller,
            PhysicalServerResourceUsageObserver usageObserver,
            PhysicalServerRoleAssociationProvider associationProvider) {
        PluginRegistry registry = bean(PluginRegistry.class)
        registry.defineDynamicExtension(PhysicalServerResourceAssignmentController.class, controller)
        registry.defineDynamicExtension(PhysicalServerResourceUsageObserver.class, usageObserver)
        registry.defineDynamicExtension(PhysicalServerRoleAssociationProvider.class, associationProvider)
        dynamicControllers.add(controller)
        dynamicUsageObservers.add(usageObserver)
        dynamicAssociationProviders.add(associationProvider)
    }

    private KvmPhysicalServerAdapter.ResourceControlAgentResponse applyResourceControlResponse(
            HttpEntity<String> entity) {
        Map<String, Object> payload = JSONObjectUtil.toObject(entity.body, LinkedHashMap.class)
        assert !payload.containsKey("operation")
        KvmPhysicalServerAdapter.ApplyResourceControlAgentCommand command =
                JSONObjectUtil.toObject(entity.body, KvmPhysicalServerAdapter.ApplyResourceControlAgentCommand.class)
        lastResourceControlCommand.set(command)
        return resourceControlResponse()
    }

    private KvmPhysicalServerAdapter.ResourceControlAgentResponse releaseResourceControlResponse(
            HttpEntity<String> entity) {
        Map<String, Object> payload = JSONObjectUtil.toObject(entity.body, LinkedHashMap.class)
        assert !payload.containsKey("operation")
        assert !payload.containsKey("cpuSet")
        assert !payload.containsKey("memory")
        assert !payload.containsKey("isolationMode")
        JSONObjectUtil.toObject(entity.body, KvmPhysicalServerAdapter.ManagedServiceAgentCommand.class)
        return resourceControlResponse()
    }

    private KvmPhysicalServerAdapter.ResourceControlAgentResponse resourceControlResponse() {
        KvmPhysicalServerAdapter.ResourceControlAgentResponse response =
                new KvmPhysicalServerAdapter.ResourceControlAgentResponse()
        resourceControlCalls.incrementAndGet()
        if (failResourceControl) {
            response.error = "simulated resource control failure"
            return response
        }

        response.synced = !mismatchResourceControl
        return response
    }

    private ResourceConsumerHandle restartableSystemdHandle(String serviceName) {
        ResourceConsumerHandle handle = new ResourceConsumerHandle()
        handle.handleType = ResourceConsumerHandle.SYSTEMD_UNIT
        handle.value = "${serviceName}.service"
        handle.serviceName = serviceName
        handle.optional = false
        handle.restartable = true
        return handle
    }

    private ManagedServiceResourceUsage managedService(String roleType, String serviceName) {
        ManagedServiceResourceUsage usage = new ManagedServiceResourceUsage()
        usage.roleType = roleType
        usage.serviceName = serviceName
        usage.restartable = false
        usage.restartRequired = false
        usage.state = "RUNNING"
        usage.cpuSet = "0-7"
        usage.cpuTime = 1000L
        usage.memory = SizeUnit.MEGABYTE.toByte(96)
        usage.memoryLimit = 0L
        return usage
    }

    private ManagedServiceResourceUsage restartableManagedService(String roleType, String serviceName) {
        ManagedServiceResourceUsage usage = managedService(roleType, serviceName)
        usage.restartable = true
        return usage
    }
}
