package org.zstack.test.integration.physicalserver

import org.springframework.http.HttpEntity
import org.zstack.core.componentloader.PluginRegistry
import org.zstack.core.db.Q
import org.zstack.header.core.Completion
import org.zstack.header.core.NoErrorCompletion
import org.zstack.header.core.ReturnValueCompletion
import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.host.HostNUMANode
import org.zstack.header.host.HostVO
import org.zstack.header.physicalserver.ManagedServiceResourceUsage
import org.zstack.header.physicalserver.PhysicalServerManager
import org.zstack.header.physicalserver.PhysicalServerResourceAssignmentObserver
import org.zstack.header.physicalserver.PhysicalServerResourceBoundary
import org.zstack.header.physicalserver.PhysicalServerResourceUsageObserver
import org.zstack.header.physicalserver.PhysicalServerResourceAssignmentFactory
import org.zstack.header.physicalserver.RoleServiceManifest
import org.zstack.physicalserver.PhysicalServerResourceAssignmentVO
import org.zstack.physicalserver.PhysicalServerVO
import org.zstack.physicalserver.PhysicalServerVO_
import org.zstack.header.physicalserver.PhysicalServerRoleType
import org.zstack.header.storage.addon.primary.ExternalPrimaryStorageVO
import org.zstack.header.storage.addon.primary.ExternalPrimaryStorageVO_
import org.zstack.kvm.KVMConstant
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KvmResourceAssignmentFactory
import org.zstack.physicalserver.PhysicalServerResourceAssignmentGlobalConfig
import org.zstack.sdk.DeletePrimaryStorageAction
import org.zstack.sdk.AddExternalPrimaryStorageAction
import org.zstack.sdk.GetPhysicalServerManagedServicesAction
import org.zstack.sdk.HostInventory
import org.zstack.sdk.PhysicalServerResourceAssignmentInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.sdk.RestartPhysicalServerManagedServicesAction
import org.zstack.sdk.SystemTagInventory
import org.zstack.sdk.UpdatePhysicalServerResourceAssignmentAction
import org.zstack.storage.zbs.AddonInfo
import org.zstack.storage.zbs.LogicalPoolInfo
import org.zstack.storage.zbs.MdsInfo
import org.zstack.storage.zbs.ZbsAgentResourceUsageProvider
import org.zstack.storage.zbs.ZbsCgroupResourceUsage
import org.zstack.storage.zbs.ZbsNodeRef
import org.zstack.storage.zbs.ZbsNodeRefContributor
import org.zstack.storage.zbs.ZbsNodeRefContributorImpl
import org.zstack.storage.zbs.ZbsPrimaryStorageMdsBase
import org.zstack.storage.zbs.ZbsResourceAssignmentFactory
import org.zstack.storage.zbs.ZbsResourceAssignmentCascadeExtension
import org.zstack.storage.zbs.ZbsStorageController
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

import static groovy.test.GroovyAssert.shouldFail
import static org.mockito.Mockito.*

class ZbsResourceUsageObservationCase extends SubCase {
    static SpringSpec springSpec = PhysicalServerTest.springSpec

    static final String SERIAL = "zbs-hci-physical-server-case"
    static final String MOVED_SERIAL = "zbs-moved-physical-server-case"
    static final String SECOND_SERIAL = "zbs-independent_%!-physical-server-case"
    static final Set<String> ZBS_CGROUPS = [
            "zstone.share.slice", "zstone.cs.slice", "zstone.vhost.slice"] as Set<String>
    static final Map<String, PhysicalServerRoleType> testRoleTypes = [:]

    EnvSpec env
    HostInventory host
    String serverUuid
    AtomicInteger providerQueryCalls = new AtomicInteger()
    volatile boolean failProviderQuery
    volatile String providerReportedSerial = SERIAL
    PhysicalServerResourceAssignmentFactory assignmentOnlyFactory
    String assignmentOnlyCpuSet = "6-7"
    PrimaryStorageInventory first
    PrimaryStorageInventory second
    String originalResourceAssignmentEnabled

    @Override
    void setup() {
        useSpring(springSpec)
    }

    @Override
    void environment() {
        env = HostEnv.oneHostEnv()
    }

    @Override
    void clean() {
        if (assignmentOnlyFactory != null) {
            bean(PluginRegistry.class).getExtensionList(PhysicalServerResourceAssignmentFactory.class)
                    .remove(assignmentOnlyFactory)
            assignmentOnlyFactory = null
        }
        PhysicalServerTest.cleanupPhysicalServerRecords()
        env.delete()
        if (originalResourceAssignmentEnabled != null) {
            PhysicalServerResourceAssignmentGlobalConfig.ENABLED.updateValue(originalResourceAssignmentEnabled)
        }
    }

    @Override
    void test() {
        env.create {
            originalResourceAssignmentEnabled = PhysicalServerResourceAssignmentGlobalConfig.ENABLED.value()
            PhysicalServerResourceAssignmentGlobalConfig.ENABLED.updateValue("false")
            host = env.inventoryByName("kvm") as HostInventory
            installKvmSimulators()
            installZbsSerialSimulator()

            first = addZbs("zbs-observation-1", "127.0.1.11")
            verifyRelationQueriesDoNotRegisterServers()
            PhysicalServerResourceAssignmentGlobalConfig.ENABLED.updateValue("true")
            bean(ZbsResourceAssignmentFactory.class).refreshAssignments()
            verifyObserverCapabilitiesAreRegisteredIndependently()
            serverUuid = physicalServerUuid(SERIAL)
            refreshResourceAssignment(serverUuid, ZbsResourceAssignmentFactory.type.toString())

            verifyAssignmentObservationDoesNotRequireUsageObservation()
            verifyContributorFailurePreservesAssignments()
            verifyInvalidAddonInfoFailsRelationQuery()
            verifyZbsCreatesReadOnlyResourceAssignment()
            verifyZstoneCgroupUsageIsObserved()
            verifyReadOnlyAssignmentRetainsLastBoundaryOnProbeFailure()
            associateHost(SERIAL)
            verifyProviderIdentityMismatchReturnsRoleError()
            verifyZbsCannotBeControlledByCloud()
            verifySerialIdentityWithoutIpFallback()
            verifyAddonInfoMoveChangesOnlyObservationRelation()
            verifyRelationRemovalDoesNotNeedProviderRelease()
        }
    }

    private void verifyRelationQueriesDoNotRegisterServers() {
        ZbsNodeRefContributorImpl refs = bean(ZbsNodeRefContributorImpl.class)
        assert refs.getAllNodesBySerialNumber()[SERIAL]?.nodeAddress == "127.0.1.11" :
                "listing ZBS nodes must work before PhysicalServer registration"
        assert refs.getNodesByServerUuids([]).isEmpty() && refs.getNodesByServerUuids(null).isEmpty() :
                "an empty selection must not request all nodes"
        assert refs.getNodesByServerUuids(["unregistered-server"]).isEmpty() :
                "unknown server UUIDs must not trigger registration"
        assert !Q.New(PhysicalServerVO.class).eq(PhysicalServerVO_.serialNumber, SERIAL).isExists() :
                "relation queries must not create PhysicalServer records"
    }

    private void verifyAssignmentObservationDoesNotRequireUsageObservation() {
        String roleType = "ASSIGNMENT_ONLY_TEST"
        PhysicalServerRoleType assignmentOnlyRole = registeredRoleType(roleType)
        assignmentOnlyFactory = [
                getRoleType: { assignmentOnlyRole }, roleServices: { new RoleServiceManifest(roleType: roleType) },
                getResourceAssignment: { String serverUuid ->
                    new AssignmentOnly(assignmentOnlyRole, assignmentOnlyCpuSet)
                }
        ] as PhysicalServerResourceAssignmentFactory
        bean(PluginRegistry.class).defineDynamicExtension(PhysicalServerResourceAssignmentFactory.class, assignmentOnlyFactory)

        refreshResourceAssignment(serverUuid, assignmentOnlyRole.toString())
        refreshPhysicalServerResourceAssignmentsFromProfile {
            serverUuids = [serverUuid]
        }
        retryInSecs {
            List<PhysicalServerResourceAssignmentInventory> current = assignments(serverUuid, roleType)
            assert current.size() == 1 && current[0].cpuSet == "6-7" && current[0].state == "Synced" :
                    "an Assignment-only Role must become Synced without a service usage observer: actual=${current}"
        }
        def services = getPhysicalServerManagedServices {
            delegate.serverUuid = serverUuid
        }.services
        assert !services.any { it.roleType == roleType } :
                "an Assignment-only Role must not fabricate managed-service usage"
        assignmentOnlyCpuSet = "1-"
        shouldFail(AssertionError) { refreshResourceAssignment(serverUuid, roleType) }
        def failed = assignments(serverUuid, roleType)[0]
        assert failed.cpuSet == "6-7" && failed.state == "Unsynced" :
                "invalid observation must preserve the last boundary and become Unsynced: actual=${failed}"
        assignmentOnlyCpuSet = "6-7"
        refreshResourceAssignment(serverUuid, roleType)
        assert assignments(serverUuid, roleType)[0].state == "Synced" :
                "valid observation must recover after parse failure"
    }

    private static class AssignmentOnly implements PhysicalServerResourceAssignmentObserver {
        final PhysicalServerRoleType roleType
        final String cpuSet

        AssignmentOnly(PhysicalServerRoleType roleType, String cpuSet) {
            this.roleType = roleType
            this.cpuSet = cpuSet
        }

        @Override
        boolean resourceExists() { return true }

        @Override
        void collectResourceAssignment(String uuid, List<String> serviceNames,
                ReturnValueCompletion<PhysicalServerResourceBoundary> completion) {
            completion.success(new PhysicalServerResourceBoundary(cpuSet: cpuSet))
        }
    }

    private static PhysicalServerRoleType registeredRoleType(String typeName) {
        PhysicalServerRoleType type = testRoleTypes[typeName]
        if (type == null) {
            type = new PhysicalServerRoleType(typeName)
            testRoleTypes[typeName] = type
        }
        return type
    }

    private void verifyObserverCapabilitiesAreRegisteredIndependently() {
        ZbsResourceAssignmentFactory factory = bean(ZbsResourceAssignmentFactory.class)
        assert bean(PluginRegistry.class).getExtensionList(PhysicalServerResourceAssignmentFactory.class).contains(factory) :
                "ZBS must register a factory, not a singleton observer"
        def assignment = factory.getResourceAssignment(physicalServerUuid(SERIAL))
        assert assignment instanceof PhysicalServerResourceAssignmentObserver :
                "ZBS must observe its Assignment boundary"
        assert assignment instanceof PhysicalServerResourceUsageObserver : "ZBS must expose service usage"
        assert !(assignment instanceof org.zstack.header.physicalserver.PhysicalServerResourceAssignmentController) :
                "ZBS must remain observation-only"
    }

    private void verifyContributorFailurePreservesAssignments() {
        String previousUuid = assignments(serverUuid, "ZBS")[0].uuid
        ZbsNodeRefContributor contributor = [getAllNodesBySerialNumber: {
            throw new IllegalStateException("simulated contributor failure")
        }] as ZbsNodeRefContributor
        PluginRegistry registry = bean(PluginRegistry.class)
        registry.defineDynamicExtension(ZbsNodeRefContributor.class, contributor)
        Throwable failure = shouldFail { bean(ZbsResourceAssignmentFactory.class).refreshAssignments() }
        assert failure.message.contains("simulated contributor failure") : "partial relation queries must fail loudly"
        assert assignments(serverUuid, "ZBS")[0].uuid == previousUuid :
                "a failed business query must not delete the persisted Assignment"
        registry.getExtensionList(ZbsNodeRefContributor.class).remove(contributor)
    }

    private void installKvmSimulators() {
        env.simulator(KVMConstant.KVM_HOST_NUMA_PATH) {
            HostNUMANode node = new HostNUMANode()
            node.nodeID = "0"
            node.cpus = ["0", "1", "2", "3", "4", "5", "6", "7"]
            node.onlineCpus = node.cpus
            node.coreGroups = [["0", "4"], ["1", "5"], ["2", "6"], ["3", "7"]]
            node.distance = ["10"]
            node.free = SizeUnit.GIGABYTE.toByte(8)
            node.size = SizeUnit.GIGABYTE.toByte(8)
            KVMAgentCommands.GetHostNUMATopologyResponse response = new KVMAgentCommands.GetHostNUMATopologyResponse()
            response.topology = ["0": node]
            return response
        }
        env.simulator(KvmResourceAssignmentFactory.APPLY_RESOURCE_CONTROL_PATH) {
            HttpEntity<String> entity ->
                KvmResourceAssignmentFactory.ApplyResourceControlAgentCommand command = JSONObjectUtil.toObject(
                                entity.body, KvmResourceAssignmentFactory.ApplyResourceControlAgentCommand.class)
                KvmResourceAssignmentFactory.ResourceControlAgentResponse response =
                        new KvmResourceAssignmentFactory.ResourceControlAgentResponse()
                response.synced = true
                return response
        }
        env.simulator(KvmResourceAssignmentFactory.RELEASE_RESOURCE_CONTROL_PATH) {
            KvmResourceAssignmentFactory.ResourceControlAgentResponse response =
                    new KvmResourceAssignmentFactory.ResourceControlAgentResponse()
            response.synced = true
            return response
        }
        env.simulator(KvmResourceAssignmentFactory.GET_MANAGED_SERVICE_USAGE_PATH) {
            HttpEntity<String> entity ->
                KvmResourceAssignmentFactory.ManagedServiceAgentCommand command =
                        JSONObjectUtil.toObject(entity.body, KvmResourceAssignmentFactory.ManagedServiceAgentCommand.class)
                KvmResourceAssignmentFactory.ManagedServiceUsageAgentResponse response =
                        new KvmResourceAssignmentFactory.ManagedServiceUsageAgentResponse()
                response.services = command.handles.collect {
                    ManagedServiceResourceUsage usage = new ManagedServiceResourceUsage()
                    usage.serviceName = it.serviceName
                    usage.state = "RUNNING"
                    usage.cpuSet = "0-7"
                    return usage
                }
                return response
        }
    }

    private void installZbsResourceUsageSimulator() {
        env.simulator(ZbsAgentResourceUsageProvider.GET_RESOURCE_USAGE_PATH) {
            HttpEntity<String> entity ->
                ZbsAgentResourceUsageProvider.ResourceUsageCommand command =
                        JSONObjectUtil.toObject(entity.body, ZbsAgentResourceUsageProvider.ResourceUsageCommand.class)
                assert command.cgroupNames.toSet() == ZBS_CGROUPS :
                        "ZBS observation must query only the canonical ZStone Slice names: " +
                                "expected=${ZBS_CGROUPS} actual=${command.cgroupNames}"
                providerQueryCalls.incrementAndGet()
                ZbsAgentResourceUsageProvider.ResourceUsageResponse response =
                        new ZbsAgentResourceUsageProvider.ResourceUsageResponse()
                if (failProviderQuery) {
                    response.success = false
                    response.error = "test provider unavailable"
                } else {
                    response.physicalServerSerialNumber = providerReportedSerial
                    response.usages = observedUsages()
                }
                return response
        }
    }

    private void installZbsSerialSimulator() {
        installZbsResourceUsageSimulator()
        env.simulator(ZbsStorageController.GET_FACTS_PATH) {
            ZbsStorageController.GetFactsRsp response = new ZbsStorageController.GetFactsRsp()
            response.uuid = "zbs-resource-observation-case"
            response.version = "1.6.1-for-test"
            response.success = true
            return response
        }
        env.simulator(ZbsStorageController.GET_CAPACITY_PATH) {
            HttpEntity<String> entity ->
                ZbsStorageController.GetCapacityCmd command =
                        JSONObjectUtil.toObject(entity.body, ZbsStorageController.GetCapacityCmd.class)
                LogicalPoolInfo pool = new LogicalPoolInfo()
                pool.logicalPoolName = command.logicalPoolNames[0]
                pool.physicalPoolName = "pool1"
                pool.capacity = SizeUnit.TERABYTE.toByte(1)
                pool.allocatedSize = SizeUnit.GIGABYTE.toByte(1)
                pool.usedSize = SizeUnit.GIGABYTE.toByte(1)
                ZbsStorageController.GetCapacityRsp response = new ZbsStorageController.GetCapacityRsp()
                response.logicalPoolInfos = [pool]
                return response
        }
        env.afterSimulator(ZbsPrimaryStorageMdsBase.SYNC_METADATA_PATH) {
            ZbsPrimaryStorageMdsBase.SyncMetadataRsp response, HttpEntity<String> entity ->
                ZbsPrimaryStorageMdsBase.SyncMetadataCmd command =
                        JSONObjectUtil.toObject(entity.body, ZbsPrimaryStorageMdsBase.SyncMetadataCmd.class)
                if (command.addr.startsWith("127.0.9.")) {
                    response.physicalServerSerialNumber = MOVED_SERIAL
                } else if (command.addr.startsWith("127.0.8.")) {
                    response.physicalServerSerialNumber = SECOND_SERIAL
                } else if (command.addr != host.managementIp) {
                    response.physicalServerSerialNumber = SERIAL
                }
                return response
        }
    }

    private void verifyZbsCreatesReadOnlyResourceAssignment() {
        String targetUuid = serverUuid
        refreshPhysicalServerResourceAssignmentsFromProfile {
            serverUuids = [targetUuid]
        }
        retryInSecs {
            List<PhysicalServerResourceAssignmentInventory> current = assignments(serverUuid, "ZBS")
            assert current.size() == 1 &&
                    current[0].cpuSet == "0-7" && current[0].memory == null && current[0].state == "Synced" :
                    "ZBS must expose one read-only Role boundary using the union of its three Slice CPU sets: " +
                            "serverUuid=${serverUuid} actual=${current}"
        }
    }

    private void verifyReadOnlyAssignmentRetainsLastBoundaryOnProbeFailure() {
        failProviderQuery = true
        CountDownLatch completed = new CountDownLatch(1)
        AtomicReference<ErrorCode> failure = new AtomicReference<>()
        AtomicInteger completions = new AtomicInteger()
        bean(PhysicalServerManager.class).refreshResourceAssignment(serverUuid, "ZBS", new Completion(null) {
            @Override
            void success() {
                completions.incrementAndGet()
                completed.countDown()
            }

            @Override
            void fail(ErrorCode errorCode) {
                failure.set(errorCode)
                completions.incrementAndGet()
                completed.countDown()
            }
        })
        assert completed.await(30, TimeUnit.SECONDS)
        assert failure.get()?.details?.contains("test provider unavailable") && completions.get() == 1 :
                "read-only Refresh must report the Provider failure once: actual=${failure.get()}"
        refreshPhysicalServerResourceAssignmentsFromProfile {
            serverUuids = [serverUuid]
        }
        retryInSecs {
            List<PhysicalServerResourceAssignmentInventory> current = assignments(serverUuid, "ZBS")
            assert current.size() == 1 && current[0].cpuSet == "0-7" && current[0].state == "Unsynced" :
                    "a failed ZBS probe must retain the last factual boundary and mark it Unsynced: " +
                            "actual=${current}"
        }
        failProviderQuery = false
        refreshPhysicalServerResourceAssignmentsFromProfile {
            serverUuids = [serverUuid]
        }
        retryInSecs {
            List<PhysicalServerResourceAssignmentInventory> current = assignments(serverUuid, "ZBS")
            assert current.size() == 1 && current[0].state == "Synced" :
                    "a later successful probe must synchronize the existing ZBS Assignment: " + "actual=${current}"
        }
    }

    private void verifyInvalidAddonInfoFailsRelationQuery() {
        String originalAddonInfo = Q.New(ExternalPrimaryStorageVO.class)
                .select(ExternalPrimaryStorageVO_.addonInfo)
                .eq(ExternalPrimaryStorageVO_.uuid, first.uuid).findValue()
        ExternalPrimaryStorageVO source = new ExternalPrimaryStorageVO(uuid: first.uuid)
        ZbsNodeRefContributorImpl contributor = bean(ZbsNodeRefContributorImpl.class)
        Throwable queryFailure = shouldFail {
            contributor.getNodesBySerialNumber([source])
        }
        assert queryFailure?.message?.contains(first.uuid) :
                "malformed addonInfo must fail the complete relation view instead of returning partial data"

        AddonInfo mixedAddonInfo = JSONObjectUtil.toObject(originalAddonInfo, AddonInfo.class)
        mixedAddonInfo.mdsInfos.add(0, null)
        source.addonInfo = JSONObjectUtil.toJsonString(mixedAddonInfo)
        def relation = contributor.getNodesBySerialNumber([source])[SERIAL]
        assert relation?.serialNumber == SERIAL && relation.nodeAddress == "127.0.1.11" :
                "one empty MDS element must not hide valid MDS relations from the same PrimaryStorage: " +
                        "actual=${relation?.nodeAddress}"
        mixedAddonInfo.mdsInfos.add(mixedAddonInfo.mdsInfos[1])
        source.addonInfo = JSONObjectUtil.toJsonString(mixedAddonInfo)
        Throwable duplicate = shouldFail { contributor.getNodesBySerialNumber([source]) }
        assert duplicate.message.contains("Multiple ZBS nodes") :
                "duplicate serials inside one PS must be rejected, not merged: actual=${duplicate}"
        source.addonInfo = "not-json"
        AtomicInteger completed = new AtomicInteger()
        bean(ZbsResourceAssignmentCascadeExtension.class).forgetAssignments([source], new NoErrorCompletion() {
            @Override
            void done() {
                completed.incrementAndGet()
            }
        })
        assert completed.get() == 1 :
                "invalid addonInfo must not block PS deletion or complete its Cascade twice: actual=${completed.get()}"
    }

    private void verifyZstoneCgroupUsageIsObserved() {
        List services = zbsServices(serverUuid)
        assert services*.serviceName.toSet() == ZBS_CGROUPS :
                "Cloud must expose the three canonical ZStone Slice names: " +
                        "expected=${ZBS_CGROUPS} actual=${services*.serviceName}"
        assert providerQueryCalls.get() > 0 :
                "managed-service display must query the ZBS Agent instead of requiring a KVM Host: " +
                        "actualQueryCalls=${providerQueryCalls.get()}"

        Map byName = services.collectEntries { [(it.serviceName): it] }
        assertUsage(
                byName["zstone.share.slice"], "0-1", 1_000L, SizeUnit.GIGABYTE.toByte(1), SizeUnit.GIGABYTE.toByte(2))
        assertUsage(byName["zstone.cs.slice"], "2-5", 2_000L, SizeUnit.GIGABYTE.toByte(3), null)
        assertUsage(
                byName["zstone.vhost.slice"], "6-7", 3_000L, SizeUnit.MEGABYTE.toByte(512), SizeUnit.GIGABYTE.toByte(1))
    }

    private void verifyProviderIdentityMismatchReturnsRoleError() {
        providerReportedSerial = MOVED_SERIAL
        retryInSecs {
            GetPhysicalServerManagedServicesAction.Result result = new GetPhysicalServerManagedServicesAction(
                            sessionId: adminSession(), serverUuid: serverUuid).call()
            assert result.error == null :
                    "one failed Role observation must not fail the complete API: actual=${result.error}"
            assert result.value.services.any { it.roleType == "COMPUTE" } :
                    "successful Roles must remain visible when ZBS observation fails: " +
                            "actual=${result.value.services}"
            assert !result.value.services.any { it.roleType == "ZBS" } :
                    "a failed ZBS observation must not fabricate service rows"
            assert result.value.roleErrors?.containsKey("ZBS") :
                    "the ZBS failure must be returned under its Role: actual=${result.value.roleErrors}"
            assert result.value.roleErrors["ZBS"].details.contains("Expected physical server serialNumber") :
                    "the Role error must retain the Provider failure: actual=${result.value.roleErrors["ZBS"]}"
        }
        providerReportedSerial = SERIAL
    }

    private void assertUsage(def service, String cpuSet, Long cpuTime, Long memory, Long memoryLimit) {
        assert service != null :
                "the configured ZStone Slice must have one display row"
        assert service.state == "RUNNING" :
                "a returned ZBS cgroup fact must be displayed as RUNNING: " +
                        "service=${service.serviceName} actual=${service.state}"
        assert service.cpuSet == cpuSet :
                "Cloud must display the Provider-reported CPU set without planning it: " +
                        "service=${service.serviceName} expected=${cpuSet} actual=${service.cpuSet}"
        assert service.cpuTime == cpuTime :
                "Cloud must display cumulative cgroup CPU time: " +
                        "service=${service.serviceName} expected=${cpuTime} actual=${service.cpuTime}"
        assert service.memory == memory :
                "Cloud must display current cgroup memory usage: " +
                        "service=${service.serviceName} expected=${memory} actual=${service.memory}"
        assert service.memoryLimit == memoryLimit :
                "Cloud must display the cgroup memory limit when ZBS reports one: " +
                        "service=${service.serviceName} expected=${memoryLimit} actual=${service.memoryLimit}"
        assert !service.restartable && !service.restartRequired :
                "an observation-only ZBS Slice must never advertise Cloud restart control: " +
                        "service=${service.serviceName} restartable=${service.restartable} " +
                        "restartRequired=${service.restartRequired}"
    }

    private void verifyZbsCannotBeControlledByCloud() {
        UpdatePhysicalServerResourceAssignmentAction update = new UpdatePhysicalServerResourceAssignmentAction(
                        sessionId: adminSession(), serverUuid: serverUuid, roleType: "ZBS", cpuSet: "2-5")
        assert update.call().error?.details?.contains("does not support resource assignment") :
                "ZBS is an observer, so the Cloud Assignment update API must reject it"

        RestartPhysicalServerManagedServicesAction restart = new RestartPhysicalServerManagedServicesAction(
                        sessionId: adminSession(),
                        serverUuid: serverUuid, roleType: "ZBS", serviceNames: ["zstone.cs.slice"])
        assert restart.call().error?.details?.contains("does not support resource assignment") :
                "ZBS is an observer, so Cloud must not restart or reconfigure its Slice"
        List<PhysicalServerResourceAssignmentInventory> current = assignments(serverUuid, "ZBS")
        assert current.size() == 1 && current[0].cpuSet == "0-7" :
                "rejected ZBS write APIs must not mutate its read-only Assignment: actual=${current}"
    }

    private void verifySerialIdentityWithoutIpFallback() {
        ZbsNodeRefContributorImpl refs = bean(ZbsNodeRefContributorImpl.class)
        def serialRef = refs.getNodesByServerUuids([serverUuid])[serverUuid]
        assert serialRef?.serialNumber == SERIAL && serialRef.nodeAddress == "127.0.1.11" :
                "a stable MDS serial must relate ZBS even when its address differs from Host IP: " +
                        "actual=${serialRef?.nodeAddress}"

        PrimaryStorageInventory missingSerial = addZbs("zbs-observation-no-serial", host.managementIp)
        retryInSecs {
            assert mdsInfos(missingSerial.uuid)[0].physicalServerSerialNumber == null :
                    "an MDS address matching a Host IP must not backfill machine identity"
            def unchanged = refs.getNodesByServerUuids([serverUuid])[serverUuid]
            assert unchanged.nodeAddress == "127.0.1.11" :
                    "a relation without Agent-reported serial must remain unassociated: " +
                            "actual=${unchanged.nodeAddress}"
        }
        assert deletePrimaryStorage(missingSerial.uuid, "Permissive").error == null :
                "a PS without reported serial must remain removable"

        refreshResourceAssignment(serverUuid, "ZBS")
        providerReportedSerial = SECOND_SERIAL
        second = addZbs("zbs-observation-independent", "127.0.8.12")
        String secondServerUuid = physicalServerUuid(SECOND_SERIAL)
        refreshResourceAssignment(secondServerUuid, "ZBS")
        assert assignments(serverUuid, "ZBS")[0].state == "Synced" :
                "adding an independent PS must not refresh the first PS using the second Agent's response"
        assert refs.getNodesByServerUuids([secondServerUuid])[secondServerUuid].nodeAddress == "127.0.8.12" :
                "each independent server must have exactly its own ZBS node address"
        def selectedRefs = spy(refs)
        Set<String> parsedStorages = [] as Set<String>
        doAnswer { invocation ->
            parsedStorages.addAll(invocation.arguments[0]*.uuid)
            invocation.callRealMethod()
        }.when(selectedRefs).getNodesBySerialNumber(anyCollection())
        assert selectedRefs.getNodesByServerUuids([serverUuid])[serverUuid]?.serialNumber == SERIAL :
                "targeted relation lookup must retain the requested server"
        assert parsedStorages == [first.uuid] as Set :
                "targeted lookup must parse only matching PS addonInfo: actual=${parsedStorages}"
        providerReportedSerial = SERIAL
    }

    private void verifyAddonInfoMoveChangesOnlyObservationRelation() {
        ZbsNodeRefContributorImpl refs = bean(ZbsNodeRefContributorImpl.class)
        String secondServerUuid = physicalServerUuid(SECOND_SERIAL)
        String secondAssignmentUuid = assignments(secondServerUuid, "ZBS")[0].uuid
        providerReportedSerial = MOVED_SERIAL
        updateExternalPrimaryStorage {
            uuid = first.uuid
            config = zbsConfig("127.0.9.14")
        }
        String movedServerUuid = physicalServerUuid(MOVED_SERIAL)
        refreshResourceAssignment(movedServerUuid, ZbsResourceAssignmentFactory.type.toString())
        retryInSecs {
            List<PhysicalServerResourceAssignmentInventory> movedAssignments = assignments(movedServerUuid, "ZBS")
            assert movedAssignments.size() == 1 &&
                    movedAssignments[0].cpuSet == "0-7" && movedAssignments[0].state == "Synced" :
                    "the moved ZBS relation must be observed on its new server: " +
                            "actual=${movedAssignments}"
        }
        retryInSecs {
            Map relations = refs.getNodesByServerUuids([serverUuid, movedServerUuid])
            assert !relations.containsKey(serverUuid) :
                    "changing the stable serial must remove the old node relation: actual=${relations}"
            assert relations[movedServerUuid]?.nodeAddress == "127.0.9.14" :
                    "changing the stable serial must create the new observation relation: " +
                            "actual=${relations[movedServerUuid]?.nodeAddress}"
        }
        retryInSecs {
            List<PhysicalServerResourceAssignmentInventory> oldAssignments = assignments(serverUuid, "ZBS")
            List<PhysicalServerResourceAssignmentInventory> movedAssignments = assignments(movedServerUuid, "ZBS")
            assert oldAssignments.isEmpty() && movedAssignments.size() == 1 && movedAssignments[0].cpuSet == "0-7" :
                    "moving a ZBS node must forget its old Assignment and create the new read-only boundary: " +
                            "old=${oldAssignments} moved=${movedAssignments}"
        }
        def unrelated = assignments(secondServerUuid, "ZBS")[0]
        assert unrelated.uuid == secondAssignmentUuid && unrelated.state == "Synced" :
                "another PS's update must not refresh or replace an independent Assignment: actual=${unrelated}"

        failProviderQuery = true
        int queriesBeforeDelete = providerQueryCalls.get()
        DeletePrimaryStorageAction.Result cleanup = deletePrimaryStorage(first.uuid, "Enforcing")
        assert cleanup.error == null :
                "forced PS deletion must not depend on the unavailable observation Provider: actual=${cleanup.error}"
        retryInSecs {
            assert assignments(movedServerUuid, "ZBS").isEmpty() :
                    "removing the moved relation must delete its read-only Assignment"
        }
        assert providerQueryCalls.get() == queriesBeforeDelete :
                "PS deletion must forget only its own Assignment without querying any ZBS Provider: " +
                        "before=${queriesBeforeDelete} after=${providerQueryCalls.get()}"
        assert assignments(secondServerUuid, "ZBS")[0].uuid == secondAssignmentUuid :
                "deleting one server's PS must not delete an independent server's Assignment"
        failProviderQuery = false
    }

    private void verifyRelationRemovalDoesNotNeedProviderRelease() {
        String secondServerUuid = physicalServerUuid(SECOND_SERIAL)
        failProviderQuery = true
        PhysicalServerResourceAssignmentGlobalConfig.ENABLED.updateValue("false")
        int queriesBeforeDelete = providerQueryCalls.get()
        DeletePrimaryStorageAction.Result deleted = deletePrimaryStorage(second.uuid, "Permissive")
        assert deleted.error == null :
                "normal PS deletion must forget its Assignment even when resource control is disabled: ${deleted.error}"
        retryInSecs {
            assert assignments(secondServerUuid, "ZBS").isEmpty() :
                    "disabled discovery must not disable Cascade cleanup of an existing ZBS Assignment"
        }
        assert providerQueryCalls.get() == queriesBeforeDelete :
                "disabled Cascade cleanup must not invoke the unavailable Provider"
        failProviderQuery = false
        PhysicalServerResourceAssignmentGlobalConfig.ENABLED.updateValue("true")
    }

    private List zbsServices(String targetServerUuid) {
        return getPhysicalServerManagedServices {
            delegate.serverUuid = targetServerUuid
        }.services.findAll { it.roleType == "ZBS" }
    }

    private PrimaryStorageInventory addZbs(String name, String address) {
        return addExternalPrimaryStorage {
            delegate.zoneUuid = host.zoneUuid
            delegate.name = name
            delegate.identity = "zbs"
            delegate.defaultOutputProtocol = "CBD"
            delegate.config = zbsConfig(address)
            delegate.url = "zbs"
        } as PrimaryStorageInventory
    }

    private String zbsConfig(String address) {
        return JSONObjectUtil.toJsonString([
                mdsUrls: ["root:password@${address}".toString()], logicalPoolName: "lpool1"])
    }

    private void associateHost(String serialNumber) {
        List<SystemTagInventory> tags = querySystemTag {
            conditions = ["resourceUuid=${host.uuid}", "tag~=systemSerialNumber::%"]
        } as List<SystemTagInventory>
        if (tags.isEmpty()) {
            createSystemTag {
                resourceUuid = host.uuid
                resourceType = "HostVO"
                tag = "systemSerialNumber::${serialNumber}"
            }
        } else {
            updateSystemTag {
                uuid = tags[0].uuid
                tag = "systemSerialNumber::${serialNumber}"
            }
        }
        bean(KvmResourceAssignmentFactory.class).afterHostConnected(
                org.zstack.header.host.HostInventory.valueOf(dbFindByUuid(host.uuid, HostVO.class)))
        retryInSecs {
            HostInventory current = (queryHost {
                conditions = ["uuid=${host.uuid}"]
            } as List<HostInventory>)[0]
            assert current.serverUuid == serverUuid :
                    "Host and ZBS serials must compose onto one PhysicalServer: " +
                            "expected=${serverUuid} actual=${current.serverUuid}"
            host = current
        }
    }

    private String physicalServerUuid(String serialNumber) {
        AtomicReference<String> found = new AtomicReference<>()
        retryInSecs {
            def servers = queryPhysicalServer {
                conditions = ["serialNumber=${serialNumber}"]
            }
            assert servers.size() == 1 :
                    "one stable serial must resolve to exactly one PhysicalServer: " +
                            "serial=${serialNumber} actual=${servers.size()}"
            found.set(servers[0].uuid)
        }
        return found.get()
    }

    private List<MdsInfo> mdsInfos(String primaryStorageUuid) {
        String addonInfo = Q.New(ExternalPrimaryStorageVO.class)
                .select(ExternalPrimaryStorageVO_.addonInfo)
                .eq(ExternalPrimaryStorageVO_.uuid, primaryStorageUuid).findValue()
        assert addonInfo != null :
                "the ZBS PrimaryStorage must exist while reading addonInfo: " +
                        "uuid=${primaryStorageUuid} actual=missing"
        return JSONObjectUtil.toObject(addonInfo, AddonInfo.class).mdsInfos
    }

    private DeletePrimaryStorageAction.Result deletePrimaryStorage(String uuid, String mode) {
        return new DeletePrimaryStorageAction(sessionId: adminSession(), uuid: uuid, deleteMode: mode).call()
    }

    private List<PhysicalServerResourceAssignmentInventory> assignments(String targetServerUuid, String roleType) {
        return queryPhysicalServerResourceAssignment {
            conditions = ["serverUuid=${targetServerUuid}", "roleType=${roleType}"]
        } as List<PhysicalServerResourceAssignmentInventory>
    }

    private void refreshResourceAssignment(String serverUuid, String roleType) {
        CountDownLatch refreshed = new CountDownLatch(1)
        AtomicReference<ErrorCode> failure = new AtomicReference<>()
        bean(PhysicalServerManager.class).refreshResourceAssignment(serverUuid, roleType, new Completion(null) {
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

    private static List<ZbsCgroupResourceUsage> observedUsages() {
        return [
                usage("zstone.share.slice", "0-1", 1_000L, SizeUnit.GIGABYTE.toByte(1), SizeUnit.GIGABYTE.toByte(2)),
                usage("zstone.cs.slice", "2-5", 2_000L, SizeUnit.GIGABYTE.toByte(3), null),
                usage("zstone.vhost.slice", "6-7", 3_000L, SizeUnit.MEGABYTE.toByte(512), SizeUnit.GIGABYTE.toByte(1))]
    }

    private static ZbsCgroupResourceUsage usage(
            String cgroupName, String cpuSet, Long cpuTime, Long memory, Long memoryLimit) {
        ZbsCgroupResourceUsage usage = new ZbsCgroupResourceUsage()
        usage.cgroupName = cgroupName
        usage.cpuSet = cpuSet
        usage.cpuTime = cpuTime
        usage.memory = memory
        usage.memoryLimit = memoryLimit
        return usage
    }
}
