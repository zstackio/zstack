package org.zstack.test.integration.physicalserver

import org.springframework.http.HttpEntity
import org.zstack.core.Platform
import org.zstack.core.componentloader.PluginRegistry
import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.header.core.ReturnValueCompletion
import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.host.HostNUMANode
import org.zstack.header.host.HostVO
import org.zstack.header.physicalserver.PhysicalServerResourceAssignmentObserver
import org.zstack.header.physicalserver.PhysicalServerResourceBoundary
import org.zstack.header.physicalserver.PhysicalServerResourceUsageObserver
import org.zstack.header.physicalserver.PhysicalServerRoleAssociationProvider
import org.zstack.header.storage.addon.primary.ExternalPrimaryStorageVO
import org.zstack.header.storage.addon.primary.ExternalPrimaryStorageVO_
import org.zstack.kvm.KVMConstant
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KvmPhysicalServerAdapter
import org.zstack.sdk.DeletePrimaryStorageAction
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
import org.zstack.storage.zbs.ZbsResourceUsageObserver
import org.zstack.storage.zbs.ZbsStorageController
import org.zstack.test.integration.kvm.host.HostEnv
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SpringSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class ZbsResourceUsageObservationCase extends SubCase {
    static SpringSpec springSpec = PhysicalServerTest.springSpec

    static final String SERIAL = "zbs-hci-physical-server-case"
    static final String MOVED_SERIAL = "zbs-moved-physical-server-case"
    static final Set<String> ZBS_CGROUPS = [
            "zstone.share.slice",
            "zstone.cs.slice",
            "zstone.vhost.slice"
    ] as Set<String>

    EnvSpec env
    HostInventory host
    String serverUuid
    AtomicInteger providerQueryCalls = new AtomicInteger()
    AtomicBoolean holdNextProviderQuery = new AtomicBoolean()
    volatile CountDownLatch providerQueryGate
    volatile CountDownLatch providerQueryStarted
    volatile boolean failProviderQuery
    volatile String providerReportedSerial = SERIAL
    PhysicalServerResourceAssignmentObserver assignmentOnlyObserver
    PhysicalServerRoleAssociationProvider assignmentOnlyAssociation
    PrimaryStorageInventory first
    PrimaryStorageInventory second

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
        try {
            holdNextProviderQuery.set(false)
            providerQueryGate?.countDown()
            providerQueryGate = null
            if (assignmentOnlyObserver != null) {
                bean(PluginRegistry.class).getExtensionList(
                        PhysicalServerResourceAssignmentObserver.class).remove(
                        assignmentOnlyObserver)
                assignmentOnlyObserver = null
            }
            if (assignmentOnlyAssociation != null) {
                bean(PluginRegistry.class).getExtensionList(
                        PhysicalServerRoleAssociationProvider.class).remove(
                        assignmentOnlyAssociation)
                assignmentOnlyAssociation = null
            }
            PhysicalServerTest.cleanupPhysicalServerRecords()
        } finally {
            env.delete()
        }
    }

    @Override
    void test() {
        env.create {
            host = env.inventoryByName("kvm") as HostInventory
            installKvmSimulators()
            installZbsSerialSimulator()

            first = addZbs("zbs-observation-1", "127.0.1.11")
            verifyObserverCapabilitiesAreRegisteredIndependently()
            bean(ZbsResourceUsageObserver.class).discoverAssociations(
                    Collections.emptySet())
            serverUuid = physicalServerUuid(SERIAL)

            verifyAssignmentObservationDoesNotRequireUsageObservation()
            verifyContributorFailurePreservesCachedRelations()
            verifyInvalidAddonInfoFailsRelationDiscovery()
            verifyZbsCreatesReadOnlyResourceAssignment()
            verifyZstoneCgroupUsageIsObserved()
            verifyReadOnlyAssignmentRetainsLastBoundaryOnProbeFailure()
            verifyConcurrentReadsUseOneProviderQuery()
            verifyProviderIdentityMismatchOnlyDegradesDisplay()
            associateHost(SERIAL)
            verifyZbsCannotBeControlledByCloud()
            verifySerialIdentityWithoutIpFallback()
            verifyAddonInfoMoveChangesOnlyObservationRelation()
            verifyRelationRemovalDoesNotNeedProviderRelease()
        }
    }

    private void verifyAssignmentObservationDoesNotRequireUsageObservation() {
        String roleType = "ASSIGNMENT_ONLY_TEST"
        assignmentOnlyObserver = new PhysicalServerResourceAssignmentObserver() {
            @Override
            String getRoleType() {
                return roleType
            }

            @Override
            void collectResourceAssignment(
                    String targetServerUuid,
                    ReturnValueCompletion<PhysicalServerResourceBoundary> completion) {
                PhysicalServerResourceBoundary boundary =
                        new PhysicalServerResourceBoundary()
                boundary.cpuSet = "6-7"
                completion.success(boundary)
            }
        }
        assert !(assignmentOnlyObserver instanceof PhysicalServerResourceUsageObserver)
        bean(PluginRegistry.class).defineDynamicExtension(
                PhysicalServerResourceAssignmentObserver.class,
                assignmentOnlyObserver)
        assignmentOnlyAssociation = new PhysicalServerRoleAssociationProvider() {
            @Override
            String getRoleType() {
                return roleType
            }

            @Override
            Set<String> discoverAssociations(Collection<String> serverUuids) {
                return !serverUuids || serverUuids.contains(serverUuid) ?
                        Collections.singleton(serverUuid) : Collections.emptySet()
            }
        }
        bean(PluginRegistry.class).defineDynamicExtension(
                PhysicalServerRoleAssociationProvider.class,
                assignmentOnlyAssociation)

        refreshPhysicalServerResourceAssignments {
            delegate.serverUuid = serverUuid
        }
        retryInSecs {
            List<PhysicalServerResourceAssignmentInventory> current =
                    assignments(serverUuid, roleType)
            assert current.size() == 1 &&
                    current[0].cpuSet == "6-7" &&
                    current[0].state == "Synced" :
                    "an Assignment-only Role must become Synced without a service usage observer: actual=${current}"
        }
        def services = getPhysicalServerManagedServices {
            delegate.serverUuid = serverUuid
        }.services
        assert !services.any { it.roleType == roleType } :
                "an Assignment-only Role must not fabricate managed-service usage"
    }

    private void verifyObserverCapabilitiesAreRegisteredIndependently() {
        assert !PhysicalServerResourceUsageObserver.isAssignableFrom(
                PhysicalServerResourceAssignmentObserver.class) :
                "assignment observation must not require managed-service usage observation"
        PluginRegistry registry = bean(PluginRegistry.class)
        ZbsResourceUsageObserver observer = bean(ZbsResourceUsageObserver.class)
        assert registry.getExtensionList(
                PhysicalServerResourceUsageObserver.class).contains(observer) :
                "ZBS must register service usage observation explicitly"
        assert registry.getExtensionList(
                PhysicalServerResourceAssignmentObserver.class).contains(observer) :
                "ZBS must register read-only Assignment observation explicitly"
    }

    private void verifyContributorFailurePreservesCachedRelations() {
        String contributedServerUuid = Platform.uuid
        ZbsNodeRef contributed = new ZbsNodeRef()
        contributed.serverUuid = contributedServerUuid
        contributed.serialNumber = "contributed-zbs-server"
        contributed.unavailableError = new ErrorCode(
                "TEST.1000", "cached relation", "cached relation")
        AtomicBoolean failDiscovery = new AtomicBoolean()
        ZbsNodeRefContributor contributor = [
                bulkList: { Collection<String> requested ->
                    if (failDiscovery.get()) {
                        throw new IllegalStateException(
                                "simulated contributor failure")
                    }
                    if (requested && !requested.contains(contributedServerUuid)) {
                        return [:]
                    }
                    return [(contributedServerUuid): contributed]
                }
        ] as ZbsNodeRefContributor
        PluginRegistry registry = bean(PluginRegistry.class)
        registry.defineDynamicExtension(ZbsNodeRefContributor.class, contributor)
        try {
            ZbsResourceUsageObserver observer = bean(ZbsResourceUsageObserver.class)
            Set<String> associated = observer.discoverAssociations(
                    Collections.singleton(contributedServerUuid))
            assert associated.contains(contributedServerUuid) :
                    "the contributor must establish its relation before failure"

            failDiscovery.set(true)
            Throwable discoveryFailure = null
            try {
                observer.discoverAssociations(
                        Collections.singleton(contributedServerUuid))
            } catch (Throwable error) {
                discoveryFailure = error
            }
            assert discoveryFailure?.message?.contains(
                    "simulated contributor failure") :
                    "an incomplete discovery must fail instead of replacing cached relations"

            AtomicReference<String> observedFailure = new AtomicReference<>()
            observer.collectResourceAssignment(
                    contributedServerUuid,
                    new ReturnValueCompletion<PhysicalServerResourceBoundary>(null) {
                        @Override
                        void success(PhysicalServerResourceBoundary ignored) {
                            observedFailure.set("unexpected success")
                        }

                        @Override
                        void fail(ErrorCode errorCode) {
                            observedFailure.set(errorCode.details)
                        }
                    })
            assert observedFailure.get() == "cached relation" :
                    "a failed refresh must retain the previous cached relation: " +
                            "actual=${observedFailure.get()}"
        } finally {
            registry.getExtensionList(ZbsNodeRefContributor.class).remove(
                    contributor)
            bean(ZbsResourceUsageObserver.class).discoverAssociations(
                    Collections.emptySet())
        }
    }

    private void installKvmSimulators() {
        env.simulator(KVMConstant.KVM_HOST_NUMA_PATH) {
            HostNUMANode node = new HostNUMANode()
            node.nodeID = "0"
            node.cpus = ["0", "1", "2", "3", "4", "5", "6", "7"]
            node.onlineCpus = node.cpus
            node.coreGroups = [
                    ["0", "4"], ["1", "5"],
                    ["2", "6"], ["3", "7"]
            ]
            node.distance = ["10"]
            node.free = SizeUnit.GIGABYTE.toByte(8)
            node.size = SizeUnit.GIGABYTE.toByte(8)
            KVMAgentCommands.GetHostNUMATopologyResponse response =
                    new KVMAgentCommands.GetHostNUMATopologyResponse()
            response.topology = ["0": node]
            return response
        }
        env.simulator(KvmPhysicalServerAdapter.APPLY_RESOURCE_CONTROL_PATH) {
            HttpEntity<String> entity ->
                KvmPhysicalServerAdapter.ResourceControlAgentCommand command =
                        JSONObjectUtil.toObject(
                                entity.body,
                                KvmPhysicalServerAdapter.ResourceControlAgentCommand.class)
                KvmPhysicalServerAdapter.ResourceControlAgentResponse response =
                        new KvmPhysicalServerAdapter.ResourceControlAgentResponse()
                response.synced = true
                return response
        }
    }

    private void installZbsResourceUsageSimulator() {
        env.simulator(ZbsAgentResourceUsageProvider.GET_RESOURCE_USAGE_PATH) {
            HttpEntity<String> entity ->
                ZbsAgentResourceUsageProvider.ResourceUsageCommand command =
                        JSONObjectUtil.toObject(
                                entity.body,
                                ZbsAgentResourceUsageProvider.ResourceUsageCommand.class)
                assert command.cgroupNames.toSet() == ZBS_CGROUPS :
                        "ZBS observation must query only the canonical ZStone Slice names: " +
                                "expected=${ZBS_CGROUPS} actual=${command.cgroupNames}"
                providerQueryCalls.incrementAndGet()
                providerQueryStarted?.countDown()
                if (holdNextProviderQuery.compareAndSet(true, false)) {
                    CountDownLatch gate = providerQueryGate
                    gate?.await(10, TimeUnit.SECONDS)
                }
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
            ZbsStorageController.GetFactsRsp response =
                    new ZbsStorageController.GetFactsRsp()
            response.uuid = "zbs-resource-observation-case"
            response.version = "1.6.1-for-test"
            response.success = true
            return response
        }
        env.simulator(ZbsStorageController.GET_CAPACITY_PATH) {
            HttpEntity<String> entity ->
                ZbsStorageController.GetCapacityCmd command =
                        JSONObjectUtil.toObject(
                                entity.body,
                                ZbsStorageController.GetCapacityCmd.class)
                LogicalPoolInfo pool = new LogicalPoolInfo()
                pool.logicalPoolName = command.logicalPoolNames[0]
                pool.physicalPoolName = "pool1"
                pool.capacity = SizeUnit.TERABYTE.toByte(1)
                pool.allocatedSize = SizeUnit.GIGABYTE.toByte(1)
                pool.usedSize = SizeUnit.GIGABYTE.toByte(1)
                ZbsStorageController.GetCapacityRsp response =
                        new ZbsStorageController.GetCapacityRsp()
                response.logicalPoolInfos = [pool]
                return response
        }
        env.afterSimulator(ZbsPrimaryStorageMdsBase.SYNC_METADATA_PATH) {
            ZbsPrimaryStorageMdsBase.SyncMetadataRsp response,
            HttpEntity<String> entity ->
                ZbsPrimaryStorageMdsBase.SyncMetadataCmd command =
                        JSONObjectUtil.toObject(
                                entity.body,
                                ZbsPrimaryStorageMdsBase.SyncMetadataCmd.class)
                if (command.addr.startsWith("127.0.9.")) {
                    response.physicalServerSerialNumber = MOVED_SERIAL
                } else if (command.addr != host.managementIp) {
                    response.physicalServerSerialNumber = SERIAL
                }
                return response
        }
    }

    private void verifyZbsCreatesReadOnlyResourceAssignment() {
        String targetUuid = serverUuid
        refreshPhysicalServerResourceAssignments {
            delegate.serverUuid = targetUuid
        }
        retryInSecs {
            List<PhysicalServerResourceAssignmentInventory> current =
                    assignments(serverUuid, "ZBS")
            assert current.size() == 1 &&
                    current[0].cpuSet == "0-7" &&
                    current[0].memory == null &&
                    current[0].state == "Synced" :
                    "ZBS must expose one read-only Role boundary using the union of its three Slice CPU sets: " +
                            "serverUuid=${serverUuid} actual=${current}"
        }
    }

    private void verifyReadOnlyAssignmentRetainsLastBoundaryOnProbeFailure() {
        failProviderQuery = true
        try {
            refreshPhysicalServerResourceAssignments {
                delegate.serverUuid = serverUuid
            }
            retryInSecs {
                List<PhysicalServerResourceAssignmentInventory> current =
                        assignments(serverUuid, "ZBS")
                assert current.size() == 1 &&
                        current[0].cpuSet == "0-7" &&
                        current[0].state == "Unsynced" :
                        "a failed ZBS probe must retain the last factual boundary and mark it Unsynced: " +
                                "actual=${current}"
            }
        } finally {
            failProviderQuery = false
        }
        refreshPhysicalServerResourceAssignments {
            delegate.serverUuid = serverUuid
        }
        retryInSecs {
            List<PhysicalServerResourceAssignmentInventory> current =
                    assignments(serverUuid, "ZBS")
            assert current.size() == 1 && current[0].state == "Synced" :
                    "a later successful probe must synchronize the existing ZBS Assignment: " +
                            "actual=${current}"
        }
    }

    private void verifyInvalidAddonInfoFailsRelationDiscovery() {
        PrimaryStorageInventory invalid = addZbs(
                "zbs-observation-invalid-addon", "127.0.1.15")
        String originalAddonInfo = Q.New(ExternalPrimaryStorageVO.class)
                .select(ExternalPrimaryStorageVO_.addonInfo)
                .eq(ExternalPrimaryStorageVO_.uuid, invalid.uuid)
                .findValue()
        assert originalAddonInfo != null
        SQL.New("update ExternalPrimaryStorageVO e set e.addonInfo = null " +
                "where e.uuid = :uuid")
                .param("uuid", invalid.uuid)
                .execute()
        try {
            ZbsNodeRefContributorImpl contributor = bean(
                    ZbsNodeRefContributorImpl.class)
            Throwable discoveryFailure = null
            try {
                contributor.bulkList([serverUuid])
            } catch (Throwable error) {
                discoveryFailure = error
            }
            assert discoveryFailure?.message?.contains(invalid.uuid) :
                    "malformed addonInfo must fail the complete relation view instead of returning partial data"

            AddonInfo mixedAddonInfo = JSONObjectUtil.toObject(
                    originalAddonInfo, AddonInfo.class)
            mixedAddonInfo.mdsInfos.add(0, null)
            SQL.New("update ExternalPrimaryStorageVO e set e.addonInfo = :addonInfo " +
                    "where e.uuid = :uuid")
                    .param("addonInfo", JSONObjectUtil.toJsonString(mixedAddonInfo))
                    .param("uuid", invalid.uuid)
                    .execute()
            def relation = contributor.bulkList([serverUuid])[serverUuid]
            assert relation.primaryStorageUuids.contains(invalid.uuid) :
                    "one empty MDS element must not hide valid MDS relations from the same PrimaryStorage: " +
                            "actual=${relation.primaryStorageUuids}"
        } finally {
            SQL.New("update ExternalPrimaryStorageVO e set e.addonInfo = :addonInfo " +
                    "where e.uuid = :uuid")
                    .param("addonInfo", originalAddonInfo)
                    .param("uuid", invalid.uuid)
                    .execute()
        }
        assert deletePrimaryStorage(invalid.uuid, "Permissive").error == null
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
                byName["zstone.share.slice"],
                "0-1", 1_000L,
                SizeUnit.GIGABYTE.toByte(1),
                SizeUnit.GIGABYTE.toByte(2))
        assertUsage(
                byName["zstone.cs.slice"],
                "2-5", 2_000L,
                SizeUnit.GIGABYTE.toByte(3),
                null)
        assertUsage(
                byName["zstone.vhost.slice"],
                "6-7", 3_000L,
                SizeUnit.MEGABYTE.toByte(512),
                SizeUnit.GIGABYTE.toByte(1))
    }

    private void verifyConcurrentReadsUseOneProviderQuery() {
        int callsBefore = providerQueryCalls.get()
        providerQueryGate = new CountDownLatch(1)
        providerQueryStarted = new CountDownLatch(1)
        holdNextProviderQuery.set(true)
        ExecutorService callers = Executors.newFixedThreadPool(2)
        List<Future<GetPhysicalServerManagedServicesAction.Result>> requests = []
        try {
            requests.add(callers.submit({
                new GetPhysicalServerManagedServicesAction(
                        sessionId: adminSession(),
                        serverUuid: serverUuid).call()
            } as Callable<GetPhysicalServerManagedServicesAction.Result>))
            assert providerQueryStarted.await(5, TimeUnit.SECONDS) :
                    "one concurrent detail query must reach the ZBS Agent"
            requests.add(callers.submit({
                new GetPhysicalServerManagedServicesAction(
                        sessionId: adminSession(),
                        serverUuid: serverUuid).call()
            } as Callable<GetPhysicalServerManagedServicesAction.Result>))
            Thread.sleep(300)
            assert requests.every { !it.done } :
                    "both concurrent detail queries must wait for the same in-flight observation"
        } finally {
            providerQueryGate.countDown()
        }
        List<GetPhysicalServerManagedServicesAction.Result> results
        try {
            results = requests.collect { it.get(5, TimeUnit.SECONDS) }
        } finally {
            callers.shutdownNow()
        }
        assert results.every {
            it.error == null &&
                    it.value.services.findAll { service ->
                        service.roleType == "ZBS"
                    }.size() == ZBS_CGROUPS.size()
        } : "both callers must receive the complete ZBS observation: actual=${results}"
        assert providerQueryCalls.get() - callsBefore == 1 :
                "concurrent reads for one PhysicalServer must issue one Provider RPC: " +
                        "before=${callsBefore} after=${providerQueryCalls.get()}"
        providerQueryGate = null
        providerQueryStarted = null
    }

    private void verifyProviderIdentityMismatchOnlyDegradesDisplay() {
        providerReportedSerial = MOVED_SERIAL
        try {
            List services = zbsServices(serverUuid)
            assert services.size() == ZBS_CGROUPS.size() &&
                    services.every { it.state == "UNAVAILABLE" } :
                    "a ZBS Agent serial mismatch must never attach facts to the wrong PhysicalServer: " +
                            "actual=${services.collect { [it.serviceName, it.state] }}"
        } finally {
            providerReportedSerial = SERIAL
        }
    }

    private void assertUsage(
            def service,
            String cpuSet,
            Long cpuTime,
            Long memory,
            Long memoryLimit) {
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
        UpdatePhysicalServerResourceAssignmentAction update =
                new UpdatePhysicalServerResourceAssignmentAction(
                        sessionId: adminSession(),
                        serverUuid: serverUuid,
                        roleType: "ZBS",
                        cpuSet: "2-5")
        assert update.call().error?.details?.contains(
                "ROLE_TYPE_NOT_SUPPORTED") :
                "ZBS is an observer, so the Cloud Assignment update API must reject it"

        RestartPhysicalServerManagedServicesAction restart =
                new RestartPhysicalServerManagedServicesAction(
                        sessionId: adminSession(),
                        serverUuid: serverUuid,
                        roleType: "ZBS",
                        serviceNames: ["zstone.cs.slice"])
        assert restart.call().error?.details?.contains(
                "ROLE_TYPE_NOT_SUPPORTED") :
                "ZBS is an observer, so Cloud must not restart or reconfigure its Slice"
        List<PhysicalServerResourceAssignmentInventory> current =
                assignments(serverUuid, "ZBS")
        assert current.size() == 1 && current[0].cpuSet == "0-7" :
                "rejected ZBS write APIs must not mutate its read-only Assignment: actual=${current}"
    }

    private void verifySerialIdentityWithoutIpFallback() {
        ZbsNodeRefContributorImpl refs = bean(
                ZbsNodeRefContributorImpl.class)
        def serialRef = refs.bulkList([serverUuid])[serverUuid]
        assert serialRef?.primaryStorageUuids?.contains(first.uuid) :
                "a stable MDS serial must relate ZBS even when its address differs from Host IP: " +
                        "primaryStorageUuid=${first.uuid} actual=${serialRef?.primaryStorageUuids}"

        second = addZbs("zbs-observation-2", "127.0.1.12")
        retryInSecs {
            List<MdsInfo> infos = mdsInfos(second.uuid)
            assert infos.size() == 1 &&
                    infos[0].physicalServerSerialNumber == SERIAL :
                    "each ZBS relation must carry the stable serial reported by its own Agent: " +
                            "expected=${SERIAL} actual=${infos*.physicalServerSerialNumber}"
        }
        def combined = refs.bulkList([serverUuid])[serverUuid]
        assert combined.primaryStorageUuids.containsAll([
                first.uuid, second.uuid
        ]) && combined.sourceRefCount == 2 :
                "multiple ZBS relations on one machine must collapse into one observation target: " +
                        "actualPrimaryStorages=${combined.primaryStorageUuids} " +
                        "actualCount=${combined.sourceRefCount}"

        PrimaryStorageInventory missingSerial = addZbs(
                "zbs-observation-no-serial", host.managementIp)
        retryInSecs {
            assert mdsInfos(missingSerial.uuid)[0]
                    .physicalServerSerialNumber == null :
                    "an MDS address matching a Host IP must not backfill machine identity"
            def unchanged = refs.bulkList([serverUuid])[serverUuid]
            assert unchanged.sourceRefCount == 2 &&
                    !unchanged.primaryStorageUuids.contains(missingSerial.uuid) :
                    "a relation without Agent-reported serial must remain unassociated: " +
                            "actual=${unchanged.primaryStorageUuids}"
        }
        assert deletePrimaryStorage(
                missingSerial.uuid, "Permissive").error == null
    }

    private void verifyAddonInfoMoveChangesOnlyObservationRelation() {
        PrimaryStorageInventory moved = addZbs(
                "zbs-observation-addon-move", "127.0.1.14")
        ZbsNodeRefContributorImpl refs = bean(
                ZbsNodeRefContributorImpl.class)
        retryInSecs {
            def oldRef = refs.bulkList([serverUuid])[serverUuid]
            assert oldRef?.primaryStorageUuids?.contains(moved.uuid) :
                    "the initial serial must associate the new ZBS relation with the existing server"
        }

        updateExternalPrimaryStorage {
            uuid = moved.uuid
            config = zbsConfig("127.0.9.14")
        }
        bean(ZbsResourceUsageObserver.class).discoverAssociations(
                Collections.emptySet())
        String movedServerUuid = physicalServerUuid(MOVED_SERIAL)
        refreshPhysicalServerResourceAssignments {
            delegate.serverUuid = serverUuid
        }
        providerReportedSerial = MOVED_SERIAL
        try {
            refreshPhysicalServerResourceAssignments {
                delegate.serverUuid = movedServerUuid
            }
            retryInSecs {
                List<PhysicalServerResourceAssignmentInventory> movedAssignments =
                        assignments(movedServerUuid, "ZBS")
                assert movedAssignments.size() == 1 &&
                        movedAssignments[0].cpuSet == "0-7" &&
                        movedAssignments[0].state == "Synced" :
                        "the moved ZBS relation must be observed before its Agent identity changes: " +
                                "actual=${movedAssignments}"
            }
        } finally {
            providerReportedSerial = SERIAL
        }
        retryInSecs {
            Map relations = refs.bulkList([serverUuid, movedServerUuid])
            assert !relations[serverUuid]?.primaryStorageUuids?.contains(
                    moved.uuid) :
                    "changing the stable serial must remove only the old observation relation: " +
                            "actual=${relations[serverUuid]?.primaryStorageUuids}"
            assert relations[movedServerUuid]?.primaryStorageUuids?.contains(
                    moved.uuid) :
                    "changing the stable serial must create the new observation relation: " +
                            "actual=${relations[movedServerUuid]?.primaryStorageUuids}"
        }
        retryInSecs {
            List<PhysicalServerResourceAssignmentInventory> oldAssignments =
                    assignments(serverUuid, "ZBS")
            List<PhysicalServerResourceAssignmentInventory> movedAssignments =
                    assignments(movedServerUuid, "ZBS")
            assert oldAssignments.size() == 1 &&
                    movedAssignments.size() == 1 &&
                    oldAssignments[0].cpuSet == "0-7" &&
                    movedAssignments[0].cpuSet == "0-7" :
                    "moving one ZBS relation must retain the old shared Role boundary and create the new read-only boundary: " +
                            "old=${oldAssignments} moved=${movedAssignments}"
        }

        DeletePrimaryStorageAction.Result cleanup =
                deletePrimaryStorage(moved.uuid, "Enforcing")
        assert cleanup.error == null :
                "the moved test PrimaryStorage must be cleanable: actual=${cleanup.error}"
        retryInSecs {
            assert refs.bulkList([movedServerUuid]).isEmpty() :
                    "a deleted ZBS PrimaryStorage must disappear from observation relations"
        }
        refreshPhysicalServerResourceAssignments {
            delegate.serverUuid = movedServerUuid
        }
        retryInSecs {
            assert assignments(movedServerUuid, "ZBS").isEmpty() :
                    "removing the moved relation must delete its read-only Assignment"
        }
    }

    private void verifyRelationRemovalDoesNotNeedProviderRelease() {
        DeletePrimaryStorageAction.Result firstDelete =
                deletePrimaryStorage(first.uuid, "Permissive")
        assert firstDelete.error == null :
                "deleting one of multiple ZBS relations must succeed without a release call: " +
                        "actual=${firstDelete.error}"
        assert zbsServices(serverUuid).size() == ZBS_CGROUPS.size() :
                "ZBS observation must remain while another relation exists"

        failProviderQuery = true
        DeletePrimaryStorageAction.Result lastDelete =
                deletePrimaryStorage(second.uuid, "Permissive")
        assert lastDelete.error == null :
                "deleting the last ZBS relation must not depend on a usage Provider or release gate: " +
                        "actual=${lastDelete.error}"
        refreshPhysicalServerResourceAssignments {
            delegate.serverUuid = serverUuid
        }
        retryInSecs {
            assert assignments(serverUuid, "ZBS").isEmpty() :
                    "removing the last relation must not leave a ZBS Assignment"
        }
        assert zbsServices(serverUuid).isEmpty() :
                "removing the last relation must remove ZBS display rows without retaining state"
        failProviderQuery = false
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
                mdsUrls: ["root:password@${address}".toString()],
                logicalPoolName: "lpool1"
        ])
    }

    private void associateHost(String serialNumber) {
        List<SystemTagInventory> tags = querySystemTag {
            conditions = [
                    "resourceUuid=${host.uuid}",
                    "tag~=systemSerialNumber::%"
            ]
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
        bean(KvmPhysicalServerAdapter.class).associate(
                org.zstack.header.host.HostInventory.valueOf(
                        dbFindByUuid(host.uuid, HostVO.class)))
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
                .eq(ExternalPrimaryStorageVO_.uuid, primaryStorageUuid)
                .findValue()
        assert addonInfo != null :
                "the ZBS PrimaryStorage must exist while reading addonInfo: " +
                        "uuid=${primaryStorageUuid} actual=missing"
        return JSONObjectUtil.toObject(
                addonInfo, AddonInfo.class).mdsInfos
    }

    private DeletePrimaryStorageAction.Result deletePrimaryStorage(
            String uuid, String mode) {
        return new DeletePrimaryStorageAction(
                sessionId: adminSession(),
                uuid: uuid,
                deleteMode: mode).call()
    }

    private List<PhysicalServerResourceAssignmentInventory> assignments(
            String targetServerUuid, String roleType) {
        return queryPhysicalServerResourceAssignment {
            conditions = [
                    "serverUuid=${targetServerUuid}",
                    "roleType=${roleType}"
            ]
        } as List<PhysicalServerResourceAssignmentInventory>
    }

    private static List<ZbsCgroupResourceUsage> observedUsages() {
        return [
                usage(
                        "zstone.share.slice", "0-1", 1_000L,
                        SizeUnit.GIGABYTE.toByte(1),
                        SizeUnit.GIGABYTE.toByte(2)),
                usage(
                        "zstone.cs.slice", "2-5", 2_000L,
                        SizeUnit.GIGABYTE.toByte(3), null),
                usage(
                        "zstone.vhost.slice", "6-7", 3_000L,
                        SizeUnit.MEGABYTE.toByte(512),
                        SizeUnit.GIGABYTE.toByte(1))
        ]
    }

    private static ZbsCgroupResourceUsage usage(
            String cgroupName,
            String cpuSet,
            Long cpuTime,
            Long memory,
            Long memoryLimit) {
        ZbsCgroupResourceUsage usage = new ZbsCgroupResourceUsage()
        usage.cgroupName = cgroupName
        usage.cpuSet = cpuSet
        usage.cpuTime = cpuTime
        usage.memory = memory
        usage.memoryLimit = memoryLimit
        return usage
    }
}
