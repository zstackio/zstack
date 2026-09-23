package org.zstack.test.integration.physicalserver

import org.springframework.http.HttpEntity
import org.zstack.compute.host.PostHostConnectExtensionPoint
import org.zstack.core.Platform
import org.zstack.core.componentloader.PluginRegistry
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.cloudbus.CloudBusEventListener
import org.zstack.core.cloudbus.EventSubscriberReceipt
import org.zstack.core.db.SQL
import org.zstack.header.core.Completion
import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.host.HostAfterConnectedExtensionPoint
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
import org.zstack.header.physicalserver.PhysicalServerResourceAssignmentFactory
import org.zstack.header.physicalserver.RoleServiceManifest
import org.zstack.physicalserver.PhysicalServerResourceAssignmentVO
import org.zstack.physicalserver.PhysicalServerManagerImpl
import org.zstack.header.message.APIEvent
import org.zstack.header.physicalserver.PhysicalServerRoleType
import org.zstack.header.physicalserver.ResourceConsumerHandle
import org.zstack.header.physicalserver.ResourceControlCommand
import org.zstack.kvm.KVMConstant
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KvmResourceAssignmentFactory
import org.zstack.portal.managementnode.LocalCpuTopologyCollector
import org.zstack.portal.managementnode.LocalResourceControlExecutor
import org.zstack.portal.managementnode.ManagementNodeResourceAssignmentFactory
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

import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.charset.StandardCharsets

import static groovy.test.GroovyAssert.shouldFail

class PhysicalServerResourceAssignmentCase extends SubCase {
    static SpringSpec springSpec = PhysicalServerTest.springSpec
    static final Map<String, PhysicalServerRoleType> testRoleTypes = [:]

    EnvSpec env
    PhysicalServerManager physicalServerManager
    HostInventory host
    HostInventory forceDeleteHost
    String physicalServerUuid
    String forceDeletePhysicalServerUuid
    volatile boolean failResourceControl
    volatile boolean mismatchResourceControl
    AtomicInteger resourceControlCalls = new AtomicInteger()
    AtomicInteger dynamicRoleCreations = new AtomicInteger()
    AtomicReference<List<String>> restartedServices = new AtomicReference<>()
    AtomicReference<KvmResourceAssignmentFactory.ApplyResourceControlAgentCommand> lastResourceControlCommand =
            new AtomicReference<>()
    AtomicReference<List<String>> inspectedServices = new AtomicReference<>()
    ManagementNodeResourceAssignmentFactory managementAdapter
    LocalCpuTopologyCollector localTopology
    LocalResourceControlExecutor localExecutor
    List<PhysicalServerResourceAssignmentFactory> dynamicFactories = []
    String originalResourceAssignmentEnabled
    Map<Path, byte[]> originalProfiles = [:]
    Map<String, AtomicInteger> updateEvents = new ConcurrentHashMap<>()
    EventSubscriberReceipt updateEventSubscription

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
        updateEventSubscription?.unsubscribeAll()
        originalProfiles.each { Path path, byte[] content -> Files.write(path, content) }
        RoleServiceManifest.reloadAll()
        if (managementAdapter != null) {
            managementAdapter.setTestSerialNumber(null)
        }
        if (localTopology != null) {
            localTopology.clearTestTopology()
        }
        PhysicalServerTest.cleanupPhysicalServerRecords()
        env.delete()
        localExecutor?.disableTestMode()
        bean(PluginRegistry.class).getExtensionList(PhysicalServerResourceAssignmentFactory.class)
                .removeAll(dynamicFactories)
        dynamicFactories.clear()
        if (originalResourceAssignmentEnabled != null) {
            PhysicalServerResourceAssignmentGlobalConfig.ENABLED.updateValue(originalResourceAssignmentEnabled)
        }
    }

    @Override
    void test() {
        env.create {
            updateEventSubscription = bean(CloudBus.class).subscribeEvent({ event ->
                def updated = (APIEvent) event
                updateEvents.computeIfAbsent(updated.apiId, { new AtomicInteger() }).incrementAndGet()
                return false
            } as CloudBusEventListener, new APIEvent())
            originalResourceAssignmentEnabled = PhysicalServerResourceAssignmentGlobalConfig.ENABLED.value()
            physicalServerManager = bean(PhysicalServerManager.class)
            host = env.inventoryByName("kvm1") as HostInventory
            forceDeleteHost = env.inventoryByName("kvm2") as HostInventory
            verifyDisabledStartupDoesNotDiscoverServers()
            PhysicalServerResourceAssignmentGlobalConfig.ENABLED.updateValue("true")
            host = associateHostThroughSerialTag(host, "physical-server-sdk-case")
            physicalServerUuid = host.serverUuid
            verifyIdentityResolution()
            verifyDuplicateHostSerialIsRejected()
            installCpuTopologySimulator()
            installResourceControlSimulator()

            verifyDynamicSharedRoleFactory()
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

    private void verifyDisabledStartupDoesNotDiscoverServers() {
        PhysicalServerResourceAssignmentGlobalConfig.ENABLED.updateValue("false")
        setHostSerialTag(host, "disabled-compute-serial")
        bean(KvmResourceAssignmentFactory.class).managementNodeReady()
        ManagementNodeResourceAssignmentFactory mn = bean(ManagementNodeResourceAssignmentFactory.class)
        mn.setTestSerialNumber("disabled-management-serial")
        mn.managementNodeReady()
        mn.setTestSerialNumber(null)
        assert (queryPhysicalServer {
            conditions = ["serialNumber?=disabled-compute-serial,disabled-management-serial"]
        }).isEmpty() : "disabled startup and serial-tag hooks must not create PhysicalServer records"
        assert dbFindByUuid(host.uuid, HostVO.class).serverUuid == null :
                "disabled discovery must not establish a Host association"
        setHostSerialTag(host, "physical-server-sdk-case")
    }

    private HostInventory associateHostThroughSerialTag(HostInventory target, String serialNumber) {
        setHostSerialTag(target, serialNumber)
        bean(KvmResourceAssignmentFactory.class).afterHostConnected(
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
        bean(KvmResourceAssignmentFactory.class).afterHostConnected(
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
        env.simulator(KvmResourceAssignmentFactory.APPLY_RESOURCE_CONTROL_PATH) {
            HttpEntity<String> entity -> applyResourceControlResponse(entity)
        }
        env.simulator(KvmResourceAssignmentFactory.RELEASE_RESOURCE_CONTROL_PATH) {
            HttpEntity<String> entity -> releaseResourceControlResponse(entity)
        }
        env.simulator(KvmResourceAssignmentFactory.GET_MANAGED_SERVICE_USAGE_PATH) {
            HttpEntity<String> entity ->
                KvmResourceAssignmentFactory.ManagedServiceAgentCommand command =
                        JSONObjectUtil.toObject(entity.body, KvmResourceAssignmentFactory.ManagedServiceAgentCommand.class)
                inspectedServices.set(command.handles.collect { it.serviceName })
                KvmResourceAssignmentFactory.ManagedServiceUsageAgentResponse response =
                        new KvmResourceAssignmentFactory.ManagedServiceUsageAgentResponse()
                response.services = command.handles.collect {
                    ManagedServiceResourceUsage usage = managedService("COMPUTE", it.serviceName)
                    usage.restartable = it.restartable
                    usage.restartRequired = it.serviceName == "node-exporter"
                    return usage
                }
                return response
        }
        env.simulator(KvmResourceAssignmentFactory.RESTART_MANAGED_SERVICES_PATH) {
            HttpEntity<String> entity ->
                KvmResourceAssignmentFactory.ManagedServiceAgentCommand command =
                        JSONObjectUtil.toObject(entity.body, KvmResourceAssignmentFactory.ManagedServiceAgentCommand.class)
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

        KvmResourceAssignmentFactory.ApplyResourceControlAgentCommand applied = lastResourceControlCommand.get()
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

        RestartPhysicalServerManagedServicesAction roleWithoutServices = new RestartPhysicalServerManagedServicesAction(
                        sessionId: adminSession(), serverUuid: physicalServerUuid, roleType: "COMPUTE")
        expect(ApiException.class) {
            roleWithoutServices.call()
        }

        RestartPhysicalServerManagedServicesAction duplicateServices = new RestartPhysicalServerManagedServicesAction(
                        sessionId: adminSession(), serverUuid: physicalServerUuid,
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

        ApiException emptyScope = shouldFail(ApiException) {
            new RefreshPhysicalServerResourceAssignmentsFromProfileAction(
                    sessionId: adminSession(), serverUuids: []).call()
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

    private void verifyDynamicSharedRoleFactory() {
        String imageStoreRoleType = "TEST_SHARED_STORAGE"
        PhysicalServerRoleType imageStoreRole = registeredRoleType(imageStoreRoleType)
        long memoryLimit = SizeUnit.MEGABYTE.toByte(256)
        AtomicInteger topologyCalls = new AtomicInteger()
        AtomicInteger applyCalls = new AtomicInteger()
        AtomicReference<Boolean> applySynced = new AtomicReference<>(true)
        AtomicBoolean throwApply = new AtomicBoolean()
        AtomicReference<Integer> defaultCpuCount = new AtomicReference<>()
        AtomicReference<Long> observedMemory = new AtomicReference<>()
        AtomicReference<ResourceControlCommand> applied = new AtomicReference<>()
        PhysicalServerResourceAssignmentController imageStore = [
                getRoleType: { imageStoreRole }, collectTopology: { String ignoredServer, def completion ->
                    topologyCalls.incrementAndGet()
                    computeResourceControlAdapter().collectTopology(ignoredServer, completion)
                }, apply: { String ignoredServer, ResourceControlCommand command, def completion ->
                    applyCalls.incrementAndGet()
                    if (throwApply.get()) {
                        throw new IllegalStateException("test Controller Apply failed before callback")
                    }
                    applied.set(command)
                    observedMemory.set(command.memory)
                    completion.success(applySynced.get())
                }, release: { String ignoredServer, ResourceControlCommand command, def completion ->
                    applyCalls.incrementAndGet()
                    applied.set(command)
                    observedMemory.set(null)
                    completion.success(true)
                }, restartManagedServices: {
                    String ignoredServer, ResourceControlCommand command, def completion ->
                        restartedServices.set(command.handles*.serviceName)
                        completion.success()
                }
        ] as PhysicalServerResourceAssignmentController
        PhysicalServerResourceUsageObserver usage = [
                getRoleType: { imageStoreRole }, collectManagedServiceUsage: {
                    String ignoredServer, ResourceControlCommand command, def completion ->
                        completion.success([restartableManagedService(imageStoreRoleType, "image-store-agent")])
                }
        ] as PhysicalServerResourceUsageObserver
        registerDynamicRole(imageStore, usage, {
            new RoleServiceManifest(roleType: imageStoreRoleType, sliceName: "zstack-test.slice",
                    defaultCpuCount: defaultCpuCount.get(), services: [restartableProfileService("image-store-agent")])
        })

        assert assignments(physicalServerUuid, imageStoreRoleType).isEmpty()
        def servicesBeforeInitialization = getPhysicalServerManagedServices {
            serverUuid = physicalServerUuid
        }.services
        assert !servicesBeforeInitialization.any { it.roleType == imageStoreRoleType } :
                "managed-service GET must not query relations or create Assignments"

        refreshResourceAssignment(physicalServerUuid, imageStoreRole.toString())
        refreshPhysicalServerResourceAssignmentsFromProfile {
            serverUuids = [physicalServerUuid]
        }
        retryInSecs {
            assert assignment(imageStoreRoleType).state == "Synced"
            assert dynamicRoleCreations.get() > 0 : "the dynamic Role must be instantiated through its factory"
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

        int creationsBeforeRefresh = dynamicRoleCreations.get()
        refreshResourceAssignment(physicalServerUuid, imageStoreRoleType)
        assert dynamicRoleCreations.get() == creationsBeforeRefresh + 1 :
                "one Refresh must reuse one Controller through association checking and Apply"

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


        applySynced.set(false)
        refreshResourceAssignment(physicalServerUuid, imageStoreRoleType)
        assert assignment(imageStoreRoleType).state == "Unsynced" :
                "Apply synced=false must finish the request but retain Unsynced until restart: " +
                        "actual=${assignment(imageStoreRoleType).state}"

        applySynced.set(null)
        def missingResult = shouldFail(AssertionError) {
            refreshResourceAssignment(physicalServerUuid, imageStoreRoleType)
        }
        assert missingResult.message.contains("returned no apply result") :
                "missing Apply result must reach the caller rather than hang the queue: actual=${missingResult.message}"
        assert assignment(imageStoreRoleType).state == "Unsynced" :
                "missing Apply result must not mark the Assignment Synced: " +
                        "actual=${assignment(imageStoreRoleType).state}"

        applySynced.set(true)
        throwApply.set(true)
        def directFailure = shouldFail(AssertionError) {
            refreshResourceAssignment(physicalServerUuid, imageStoreRoleType)
        }
        assert directFailure.message.contains("test Controller Apply failed before callback") :
                "native async handling must report a Controller exception: actual=${directFailure.message}"
        String failingApiId = Platform.getUuid()
        def failedUpdate = new UpdatePhysicalServerResourceAssignmentAction(
                sessionId: adminSession(), apiId: failingApiId,
                serverUuid: physicalServerUuid, roleType: imageStoreRoleType, memory: memoryLimit).call()
        assert failedUpdate.error?.details?.contains("test Controller Apply failed before callback") :
                "Controller exception must reach the Update Event: actual=${failedUpdate.error}"
        retryInSecs {
            assert updateEvents[failingApiId]?.get() == 1 : "Controller exception must publish exactly one error Event"
        }
        throwApply.set(false)
        refreshResourceAssignment(physicalServerUuid, imageStoreRoleType)
        assert assignment(imageStoreRoleType).state == "Synced" :
                "the next request must recover after Controller failure without a retry wrapper: " +
                        "actual=${assignment(imageStoreRoleType).state}"

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

        int topologyCallsBeforeUpdate = topologyCalls.get()
        def previousLastOpDate = assignment(imageStoreRoleType).lastOpDate
        sleep(1100) // Database timestamps have second precision.
        def cpuUpdate = updatePhysicalServerResourceAssignment {
            serverUuid = physicalServerUuid
            roleType = imageStoreRoleType
            cpuSet = "8-11"
        }
        assert cpuUpdate.lastOpDate == assignment(imageStoreRoleType).lastOpDate :
                "Update must return the database-generated lastOpDate after Apply: actual=${cpuUpdate.lastOpDate}"
        assert cpuUpdate.lastOpDate.after(previousLastOpDate) :
                "Update must not return the pre-update timestamp: before=${previousLastOpDate} " +
                        "after=${cpuUpdate.lastOpDate}"
        assert topologyCalls.get() == topologyCallsBeforeUpdate + 1 :
                "Update validation and Apply must share one topology: before=${topologyCallsBeforeUpdate} " +
                        "after=${topologyCalls.get()}"
        refreshResourceAssignment(physicalServerUuid, imageStoreRoleType)
        assert topologyCalls.get() == topologyCallsBeforeUpdate + 2 :
                "A new Refresh must collect fresh topology instead of retaining the previous request's snapshot: " +
                        "before=${topologyCallsBeforeUpdate} after=${topologyCalls.get()}"
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
        verifyReleaseAndForgetDeletesAssignment(imageStoreRoleType)
    }

    private void verifyInvalidUpdateDoesNotChangeAssignment() {
        PhysicalServerResourceAssignmentInventory before = assignment()

        UpdatePhysicalServerResourceAssignmentAction invalidCpu = new UpdatePhysicalServerResourceAssignmentAction(
                        sessionId: adminSession(),
                        serverUuid: physicalServerUuid, roleType: "COMPUTE", cpuSet: "999999")
        UpdatePhysicalServerResourceAssignmentAction.Result cpuResult = invalidCpu.call()
        assert cpuResult.error != null
        assert cpuResult.error.details.contains("outside the online topology")

        UpdatePhysicalServerResourceAssignmentAction invalidMemory = new UpdatePhysicalServerResourceAssignmentAction(
                        sessionId: adminSession(), serverUuid: physicalServerUuid, roleType: "COMPUTE", memory: -1L)
        UpdatePhysicalServerResourceAssignmentAction.Result memoryResult = invalidMemory.call()
        assert memoryResult.error != null
        assert memoryResult.error.details.contains("positive multiple of 1 MiB")

        UpdatePhysicalServerResourceAssignmentAction unalignedMemory = new UpdatePhysicalServerResourceAssignmentAction(
                        sessionId: adminSession(), serverUuid: physicalServerUuid, roleType: "COMPUTE", memory: 1L)
        def unalignedMemoryResult = unalignedMemory.call()
        assert unalignedMemoryResult.error?.details?.contains("positive multiple of 1 MiB") :
                "memory must use the platform byte unit with 1 MiB alignment: " +
                        "actual=${unalignedMemoryResult.error}"

        UpdatePhysicalServerResourceAssignmentAction empty = new UpdatePhysicalServerResourceAssignmentAction(
                        sessionId: adminSession(), serverUuid: physicalServerUuid, roleType: "COMPUTE")
        UpdatePhysicalServerResourceAssignmentAction.Result emptyResult = empty.call()
        assert emptyResult.error != null
        assert emptyResult.error.details.contains("CpuSet or memory must be specified")

        UpdatePhysicalServerResourceAssignmentAction unsupportedRole = new UpdatePhysicalServerResourceAssignmentAction(
                        sessionId: adminSession(),
                        serverUuid: physicalServerUuid, roleType: "UNKNOWN_ROLE", cpuSet: "0")
        assert unsupportedRole.call().error != null :
                "an unregistered Role must be rejected"
        assert assignments(physicalServerUuid, "UNKNOWN_ROLE").isEmpty() :
                "an unregistered Role must not create an Assignment implicitly"

        UpdatePhysicalServerResourceAssignmentAction missingAssignment =
                new UpdatePhysicalServerResourceAssignmentAction(
                        sessionId: adminSession(), serverUuid: physicalServerUuid, roleType: "TEST_SHARED_STORAGE", cpuSet: "0")
        assert missingAssignment.call().error?.details?.contains("does not exist on physical server") :
                "a registered Adapter without an owner relation must not recreate deleted configuration"

        RestartPhysicalServerManagedServicesAction invalidServiceName = new RestartPhysicalServerManagedServicesAction(
                        sessionId: adminSession(),
                        serverUuid: physicalServerUuid, roleType: "COMPUTE", serviceNames: ["bad service name"])
        assert invalidServiceName.call().error?.details?.contains("Service names must be non-empty and unique") :
                "service restart accepts stable service identities only"

        RestartPhysicalServerManagedServicesAction tooManyServices = new RestartPhysicalServerManagedServicesAction(
                        sessionId: adminSession(),
                        serverUuid: physicalServerUuid, roleType: "COMPUTE", serviceNames: (0..64).collect {
                            "service-${it}".toString()
                        })
        assert tooManyServices.call().error?.details?.contains("At most 64 services") :
                "one restart request must remain bounded to 64 services"

        RestartPhysicalServerManagedServicesAction undefinedService = new RestartPhysicalServerManagedServicesAction(
                        sessionId: adminSession(),
                        serverUuid: physicalServerUuid, roleType: "COMPUTE", serviceNames: ["undefined-service"])
        assert undefinedService.call().error?.details?.contains("are not managed by roleType") :
                "a syntactically valid but unconfigured service must be rejected by the Role manifest"

        env.simulator(KVMConstant.KVM_HOST_NUMA_PATH) {
            HostNUMANode node = new HostNUMANode()
            node.nodeID = "0"
            node.cpus = ["0", "1"]
            node.onlineCpus = node.cpus
            node.coreGroups = [["0"]]
            KVMAgentCommands.GetHostNUMATopologyResponse response = new KVMAgentCommands.GetHostNUMATopologyResponse()
            response.topology = ["0": node]
            return response
        }
        UpdatePhysicalServerResourceAssignmentAction invalidTopology = new UpdatePhysicalServerResourceAssignmentAction(
                        sessionId: adminSession(), serverUuid: physicalServerUuid, roleType: "COMPUTE", cpuSet: "0")
        def error = invalidTopology.call().error
        assert error?.code == "SYS.1000"
        assert error.details.contains("Core groups do not cover every online CPU exactly once") :
                "Cloud must reject an incomplete Host topology before changing the ledger"
        env.simulator(KVMConstant.KVM_HOST_NUMA_PATH) {
            return validTopologyResponse()
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
        CountDownLatch entered = new CountDownLatch(1)
        CountDownLatch finishApply = new CountDownLatch(1)
        env.simulator(KvmResourceAssignmentFactory.APPLY_RESOURCE_CONTROL_PATH) { HttpEntity<String> entity ->
            entered.countDown()
            assert finishApply.await(30, TimeUnit.SECONDS)
            return applyResourceControlResponse(entity)
        }
        String sessionUuid = adminSession()
        String delayedApiId = Platform.getUuid()
        CompletableFuture<UpdatePhysicalServerResourceAssignmentAction.Result> pending = CompletableFuture.supplyAsync({
            new UpdatePhysicalServerResourceAssignmentAction(sessionId: sessionUuid, apiId: delayedApiId,
                    serverUuid: physicalServerUuid, roleType: "COMPUTE", memory: SizeUnit.MEGABYTE.toByte(56)).call()
        } as java.util.function.Supplier)
        boolean agentEntered = entered.await(30, TimeUnit.SECONDS)
        Thread.sleep(200)
        boolean repliedBeforeApply = pending.isDone()
        boolean publishedBeforeApply = updateEvents.containsKey(delayedApiId)
        finishApply.countDown()
        def applied = pending.get(30, TimeUnit.SECONDS)
        installResourceControlSimulator()
        assert agentEntered && !repliedBeforeApply : "Update must not publish a terminal Event before Agent completion"
        assert !publishedBeforeApply : "Update must wait for Apply before publishing, regardless of SDK poll timing"
        assert applied.error == null && applied.value.inventory.state == "Synced" :
                "successful Update must return the post-Apply state: actual=${applied.error ?: applied.value.inventory}"
        retryInSecs {
            assert updateEvents[delayedApiId]?.get() == 1 : "successful Apply must publish exactly one Event"
        }

        failResourceControl = true
        int callsBeforeFailure = resourceControlCalls.get()
        String failedApiId = Platform.getUuid()
        def failed = new UpdatePhysicalServerResourceAssignmentAction(sessionId: sessionUuid, apiId: failedApiId,
                serverUuid: physicalServerUuid, roleType: "COMPUTE", cpuSet: "0-1",
                memory: SizeUnit.MEGABYTE.toByte(64)).call()
        assert failed.error?.details?.contains("simulated resource control failure") :
                "Apply failure must reach the API caller: actual=${failed.error}"
        retryInSecs {
            assert updateEvents[failedApiId]?.get() == 1 : "Agent failure must publish exactly one error Event"
        }
        PhysicalServerResourceAssignmentInventory updated = assignment()
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
        PhysicalServerResourceAssignmentInventory pendingRestart = updatePhysicalServerResourceAssignment {
            serverUuid = physicalServerUuid
            roleType = "COMPUTE"
            memory = SizeUnit.MEGABYTE.toByte(80)
        }
        assert pendingRestart.state == "Unsynced" : "a staged restart is a successful Update, not an execution error"
        retryInSecs {
            assert assignment().state == "Unsynced" :
                    "an HTTP-success response with synced=false must keep the Assignment Unsynced"
        }
        mismatchResourceControl = false
        int callsBeforeHostConnected = resourceControlCalls.get()
        int roleCreationsBeforeHostConnected = dynamicRoleCreations.get()
        List<HostAfterConnectedExtensionPoint> connectedExtensions =
                bean(PluginRegistry.class).getExtensionList(HostAfterConnectedExtensionPoint.class)
        HostAfterConnectedExtensionPoint connectedExtension =
                connectedExtensions.find { it instanceof KvmResourceAssignmentFactory }
        assert connectedExtension != null : "KVM resource assignment must register for Host Connected events"
        assert !bean(PluginRegistry.class).getExtensionList(PostHostConnectExtensionPoint.class).any {
            it instanceof KvmResourceAssignmentFactory
        } : "KVM resource assignment must run only after Host reaches Connected"
        def connectedHost = org.zstack.header.host.HostInventory.valueOf(dbFindByUuid(host.uuid, HostVO.class))
        connectedExtension.afterHostConnected(connectedHost)
        retryInSecs {
            assert resourceControlCalls.get() > callsBeforeHostConnected :
                    "Host Connected must retry an Assignment that could not be applied while the Host was Connecting"
            assert assignment().state == "Synced" :
                    "the same Assignment must recover without a manual Refresh after Host Connected"
            assert dynamicRoleCreations.get() == roleCreationsBeforeHostConnected :
                    "Host Connected must refresh only the COMPUTE Role"
        }
    }

    private void verifyConcurrentSparseUpdates() {
        CountDownLatch start = new CountDownLatch(1)
        String sessionUuid = adminSession()

        CompletableFuture<Void> cpuUpdate = CompletableFuture.runAsync({
            start.await()
            UpdatePhysicalServerResourceAssignmentAction.Result result =
                    new UpdatePhysicalServerResourceAssignmentAction(sessionId: sessionUuid,
                            serverUuid: physicalServerUuid, roleType: "COMPUTE", cpuSet: "0-2").call()
            assert result.error == null : result.error
        } as Runnable)
        CompletableFuture<Void> memoryUpdate = CompletableFuture.runAsync({
            start.await()
            UpdatePhysicalServerResourceAssignmentAction.Result result =
                    new UpdatePhysicalServerResourceAssignmentAction(sessionId: sessionUuid,
                            serverUuid: physicalServerUuid,
                            roleType: "COMPUTE", memory: SizeUnit.MEGABYTE.toByte(96)).call()
            assert result.error == null : result.error
        } as Runnable)

        start.countDown()
        CompletableFuture.allOf(cpuUpdate, memoryUpdate).get(30, TimeUnit.SECONDS)
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
        int callsBeforeDisable = resourceControlCalls.get()
        PhysicalServerResourceAssignmentGlobalConfig.ENABLED.updateValue("false")
        retryInSecs {
            assert resourceControlCalls.get() > callsBeforeDisable :
                    "disabling must remove live CPU and memory limits through the Role controller"
        }
        KvmResourceAssignmentFactory adapter = bean(KvmResourceAssignmentFactory.class)
        int callsBeforeDisabledReconnect = resourceControlCalls.get()
        int roleCreationsBeforeDisabledReconnect = dynamicRoleCreations.get()
        adapter.afterHostConnected(org.zstack.header.host.HostInventory.valueOf(dbFindByUuid(host.uuid, HostVO.class)))
        retryInSecs {
            assert resourceControlCalls.get() > callsBeforeDisabledReconnect :
                    "a disabled reconnect must remove stale limits without association queries"
            assert dynamicRoleCreations.get() == roleCreationsBeforeDisabledReconnect :
                    "a disabled reconnect must not instantiate unassociated Roles"
        }
        refreshPhysicalServerResourceAssignmentsFromProfile {
            serverUuids = [physicalServerUuid]
        }
        retryInSecs {
            assert assignment().state == "Unsynced" :
                    "disabled resource assignment must retain the ledger without applying it"
        }

        UpdatePhysicalServerResourceAssignmentAction.Result result = new UpdatePhysicalServerResourceAssignmentAction(
                        sessionId: adminSession(),
                        serverUuid: physicalServerUuid, roleType: "COMPUTE", cpuSet: "0-1").call()
        assert result.error?.details?.contains("Resource assignment is disabled") :
                "disabled resource assignment must reject enforcement updates: " + "actual=${result.error}"
        PhysicalServerResourceAssignmentGlobalConfig.ENABLED.updateValue("true")

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
        PhysicalServerResourceAssignmentController exclusiveStorage = [
                getRoleType: { storageRole }, collectTopology: { String serverUuid, def completion ->
                    computeResourceControlAdapter().collectTopology(serverUuid, completion)
                }, apply: { String ignoredServer, ResourceControlCommand command, def completion ->
                    completion.success(true)
                }, release: { String ignoredServer, ResourceControlCommand command, def completion ->
                    completion.success(true)
                }, restartManagedServices: {
                    String ignoredServer, ResourceControlCommand command, def completion ->
                        restartedServices.set(command.handles*.serviceName)
                        completion.success()
                }
        ] as PhysicalServerResourceAssignmentController
        PhysicalServerResourceUsageObserver usage = [
                getRoleType: { storageRole }, collectManagedServiceUsage: {
                    String ignoredServer, ResourceControlCommand command, def completion ->
                        completion.success([restartableManagedService(storageRoleType, "test-storage")])
                }
        ] as PhysicalServerResourceUsageObserver
        registerDynamicRole(exclusiveStorage, usage, {
            new RoleServiceManifest(roleType: storageRoleType, sliceName: "zstack-test.slice", defaultCpuCount: 4,
                    isolationMode: PhysicalServerResourceIsolationMode.EXCLUSIVE,
                    services: [restartableProfileService("test-storage")])
        })
        refreshResourceAssignment(physicalServerUuid, storageRole.toString())
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

        UpdatePhysicalServerResourceAssignmentAction splitCore = new UpdatePhysicalServerResourceAssignmentAction(
                        sessionId: adminSession(),
                        serverUuid: physicalServerUuid, roleType: storageRoleType, cpuSet: "2")
        assert splitCore.call().error?.details?.contains("CPU set splits core group") :
                "an exclusive Role must reserve every sibling of a physical core"

        UpdatePhysicalServerResourceAssignmentAction cpuZero = new UpdatePhysicalServerResourceAssignmentAction(
                        sessionId: adminSession(),
                        serverUuid: physicalServerUuid, roleType: storageRoleType, cpuSet: "0,4")
        assert cpuZero.call().error?.details?.contains("CPU0 core group must remain shared") :
                "CPU0 must remain outside every exclusive service boundary"

        PhysicalServerResourceAssignmentInventory before = assignment()
        UpdatePhysicalServerResourceAssignmentAction overlap = new UpdatePhysicalServerResourceAssignmentAction(
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

        verifyReleaseAndForgetDeletesAssignment(storageRoleType)
    }

    private void verifySharedHandleOwnership() {
        managementAdapter = bean(ManagementNodeResourceAssignmentFactory.class)
        localTopology = bean(LocalCpuTopologyCollector.class)
        localExecutor = bean(LocalResourceControlExecutor.class)
        managementAdapter.setTestSerialNumber("physical-server-sdk-case")
        SQL.New("update ManagementNodeVO m set m.serverUuid = null " +
                "where m.uuid = :uuid").param("uuid", Platform.getManagementServerId()).execute()
        localTopology.setTestTopology(PhysicalServerCpuTopology.from([
                "0": new org.zstack.header.physicalserver.PhysicalServerNumaNode(nodeId: "0",
                        onlineCpus: (0..15).collect { it.toString() }, coreGroups: [
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
        assert localExecutor.testCalls > 0 :
                "the MANAGEMENT Role must apply independently from the COMPUTE Role"
        verifyProfileMemoryDefaults()
    }

    private void verifyProfileMemoryDefaults() {
        [COMPUTE: "compute.yaml", MANAGEMENT: "management.yaml"].each { String role, String file ->
            Path profile = Paths.get(getClass().classLoader.getResource("physical-server-roles/${file}").toURI())
            byte[] original = Files.readAllBytes(profile)
            originalProfiles.put(profile, original)
            String yaml = new String(original, StandardCharsets.UTF_8).replaceAll("(?m)^defaultMemory:.*\\R?", "")
            Files.write(profile, (yaml + "\ndefaultMemory: 2097152\n").getBytes(StandardCharsets.UTF_8))
            refreshPhysicalServerResourceAssignmentsFromProfile { serverUuids = [physicalServerUuid] }
            retryInSecs {
                assert assignment(physicalServerUuid, role).memory == 2097152L :
                        "FromProfile must use ${role} factory memory defaults"
                assert assignment(physicalServerUuid, role).state == "Synced"
            }
            updatePhysicalServerResourceAssignment {
                serverUuid = physicalServerUuid
                roleType = role
                memory = 3145728L
            }
            refreshResourceAssignment(physicalServerUuid, role)
            assert assignment(physicalServerUuid, role).memory == 3145728L :
                    "ordinary reconnect Refresh must preserve user memory instead of reapplying Profile"
            Files.write(profile, (yaml + "\ndefaultMemory: 4194304\n").getBytes(StandardCharsets.UTF_8))
            refreshPhysicalServerResourceAssignmentsFromProfile { serverUuids = [physicalServerUuid] }
            retryInSecs {
                assert assignment(physicalServerUuid, role).memory == 4194304L :
                        "explicit FromProfile must apply changed ${role} memory"
                assert assignment(physicalServerUuid, role).state == "Synced"
            }
            Files.write(profile, original)
            refreshPhysicalServerResourceAssignmentsFromProfile { serverUuids = [physicalServerUuid] }
            refreshResourceAssignment(physicalServerUuid, role)
            retryInSecs {
                assert assignment(physicalServerUuid, role).memory == 4194304L :
                        "omitting defaultMemory must preserve the existing ${role} Assignment memory"
                assert assignment(physicalServerUuid, role).state == "Synced"
            }
        }
        RoleServiceManifest.reloadAll()
    }

    private void verifyHostCascadeCleanup() {
        failResourceControl = true
        DeleteHostAction.Result deletedAfterReleaseFailure = new DeleteHostAction(
                sessionId: adminSession(), uuid: host.uuid, deleteMode: "Permissive").call()
        assert deletedAfterReleaseFailure.error == null :
                "Host deletion must forget the Assignment when runtime release fails: " +
                        "hostUuid=${host.uuid} actual=${deletedAfterReleaseFailure.error}"
        retryInSecs {
            assert (queryHost {
                conditions = ["uuid=${host.uuid}"]
            }).isEmpty() :
                    "release failure must not block Host deletion: " + "hostUuid=${host.uuid} actual=still present"
            assert assignments(physicalServerUuid, "COMPUTE").isEmpty() :
                    "release failure must still forget the COMPUTE Assignment: " +
                            "serverUuid=${physicalServerUuid} actual=${assignments(physicalServerUuid, 'COMPUTE')}"
        }

        failResourceControl = false
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
        int callsAfterDeletion = resourceControlCalls.get()
        verifyMissingAssignment(physicalServerUuid, "COMPUTE")
        verifyMissingAssignment(forceDeletePhysicalServerUuid, "COMPUTE")
        assert assignments(physicalServerUuid, "COMPUTE").isEmpty() :
                "a delayed Refresh must not recreate the deleted Host's Assignment"
        assert assignments(forceDeletePhysicalServerUuid, "COMPUTE").isEmpty() :
                "a delayed Refresh must not undo enforcing cascade cleanup"
        assert resourceControlCalls.get() == callsAfterDeletion : "removed Roles must not send Agent commands"
    }

    private void verifyMissingAssignment(String serverUuid, String roleType) {
        [refresh: physicalServerManager.&refreshResourceAssignment,
         release: physicalServerManager.&releaseResourceAssignment,
         forget: physicalServerManager.&forgetResourceAssignment].each { String operation, Closure invoke ->
            CountDownLatch completed = new CountDownLatch(1)
            AtomicReference<ErrorCode> failure = new AtomicReference<>()
            invoke(serverUuid, roleType, new Completion(null) {
                @Override
                void success() {
                    completed.countDown()
                }

                @Override
                void fail(ErrorCode errorCode) {
                    failure.set(errorCode)
                    completed.countDown()
                }
            })
            assert completed.await(30, TimeUnit.SECONDS) : "${operation} must complete after Role removal"
            ErrorCode error = failure.get()
            assert error != null && error.details.contains("does not exist") &&
                    error.details.contains(serverUuid) && error.details.contains(roleType) :
                    "${operation} must report the missing Assignment instead of success: actual=${error}"
        }
    }

    private void verifyReleaseAndForgetDeletesAssignment(String roleType) {
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
        assert !assignments(physicalServerUuid, roleType).isEmpty() :
                "release must remove runtime limits without deleting the Assignment"

        CountDownLatch forgotten = new CountDownLatch(1)
        physicalServerManager.forgetResourceAssignment(physicalServerUuid, roleType, new Completion(null) {
            @Override
            void success() {
                forgotten.countDown()
            }

            @Override
            void fail(ErrorCode errorCode) {
                failure.set(errorCode)
                forgotten.countDown()
            }
        })
        assert forgotten.await(30, TimeUnit.SECONDS)
        assert failure.get() == null
        List<PhysicalServerResourceAssignmentInventory> assignments = queryPhysicalServerResourceAssignment {
            conditions = ["serverUuid=${physicalServerUuid}", "roleType=${roleType}"]
        }
        assert assignments.isEmpty()
    }

    private void refreshResourceAssignment(String serverUuid, String roleType) {
        CountDownLatch refreshed = new CountDownLatch(1)
        AtomicReference<ErrorCode> failure = new AtomicReference<>()
        physicalServerManager.refreshResourceAssignment(serverUuid, roleType, new Completion(null) {
            @Override
            void success() {
                refreshed.countDown()
            }

            @Override
            void fail(ErrorCode errorCode) {
                failure.set(errorCode)
                refreshed.countDown()
            }
        })

        assert refreshed.await(30, TimeUnit.SECONDS)
        assert failure.get() == null
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
        PhysicalServerResourceAssignmentVO vo =
                dbFindByUuid(assignment().uuid, PhysicalServerResourceAssignmentVO.class)
        return bean(PhysicalServerManagerImpl.class).getResourceAssignment(vo.serverUuid, vo.roleType) as
                PhysicalServerResourceAssignmentController
    }

    private static PhysicalServerRoleType registeredRoleType(String typeName) {
        PhysicalServerRoleType type = testRoleTypes[typeName]
        if (type == null) {
            type = new PhysicalServerRoleType(typeName)
            testRoleTypes[typeName] = type
        }
        return type
    }

    private void registerDynamicRole(
            PhysicalServerResourceAssignmentController controller, PhysicalServerResourceUsageObserver usage,
            Closure<RoleServiceManifest> profile) {
        PhysicalServerResourceAssignmentFactory factory = [
                getRoleType: { controller.roleType },
                roleServices: profile,
                getResourceAssignment: { String serverUuid ->
                    dynamicRoleCreations.incrementAndGet()
                    new TestRoleController(controller, usage)
                }
        ] as PhysicalServerResourceAssignmentFactory
        bean(PluginRegistry.class).defineDynamicExtension(PhysicalServerResourceAssignmentFactory.class, factory)
        dynamicFactories.add(factory)
    }

    private static class TestRoleController {
        @Override
        boolean resourceExists() { return true }

        @Delegate
        PhysicalServerResourceAssignmentController controller
        @Delegate
        PhysicalServerResourceUsageObserver usage

        TestRoleController(PhysicalServerResourceAssignmentController controller,
                PhysicalServerResourceUsageObserver usage) {
            this.controller = controller
            this.usage = usage
        }
    }

    private KvmResourceAssignmentFactory.ResourceControlAgentResponse applyResourceControlResponse(
            HttpEntity<String> entity) {
        Map<String, Object> payload = JSONObjectUtil.toObject(entity.body, LinkedHashMap.class)
        assert !payload.containsKey("operation")
        KvmResourceAssignmentFactory.ApplyResourceControlAgentCommand command =
                JSONObjectUtil.toObject(entity.body, KvmResourceAssignmentFactory.ApplyResourceControlAgentCommand.class)
        lastResourceControlCommand.set(command)
        return resourceControlResponse()
    }

    private KvmResourceAssignmentFactory.ResourceControlAgentResponse releaseResourceControlResponse(
            HttpEntity<String> entity) {
        Map<String, Object> payload = JSONObjectUtil.toObject(entity.body, LinkedHashMap.class)
        assert !payload.containsKey("operation")
        assert !payload.containsKey("cpuSet")
        assert !payload.containsKey("memory")
        assert !payload.containsKey("isolationMode")
        JSONObjectUtil.toObject(entity.body, KvmResourceAssignmentFactory.ManagedServiceAgentCommand.class)
        return resourceControlResponse()
    }

    private KvmResourceAssignmentFactory.ResourceControlAgentResponse resourceControlResponse() {
        KvmResourceAssignmentFactory.ResourceControlAgentResponse response =
                new KvmResourceAssignmentFactory.ResourceControlAgentResponse()
        resourceControlCalls.incrementAndGet()
        if (failResourceControl) {
            response.error = "simulated resource control failure"
            return response
        }

        response.synced = !mismatchResourceControl
        return response
    }

    private RoleServiceManifest.Service restartableProfileService(String serviceName) {
        return new RoleServiceManifest.Service(name: serviceName, handleType: ResourceConsumerHandle.SYSTEMD_UNIT,
                value: serviceName + ".service", required: true, restartable: true)
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
