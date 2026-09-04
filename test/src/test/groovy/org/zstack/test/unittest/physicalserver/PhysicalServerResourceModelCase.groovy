package org.zstack.test.unittest.physicalserver

import org.junit.Test
import org.zstack.core.Platform
import org.zstack.core.componentloader.PluginRegistry
import org.zstack.core.config.GlobalConfigDef
import org.zstack.header.message.APIParam
import org.zstack.header.physicalserver.PhysicalServerCpuSet
import org.zstack.header.physicalserver.PhysicalServerCpuTopology
import org.zstack.header.physicalserver.PhysicalServerNumaNode
import org.zstack.header.physicalserver.PhysicalServerResourceAssignmentController
import org.zstack.header.physicalserver.PhysicalServerResourceAssignmentObserver
import org.zstack.header.physicalserver.PhysicalServerResourceIsolationMode
import org.zstack.header.physicalserver.PhysicalServerResourceUsageObserver
import org.zstack.header.physicalserver.PhysicalServerRoleAssociationProvider
import org.zstack.header.physicalserver.PhysicalServerRoleType
import org.zstack.header.physicalserver.ResourceConsumerHandle
import org.zstack.header.physicalserver.RoleServiceManifest
import org.zstack.header.rest.RestRequest
import org.zstack.physicalserver.APIRefreshPhysicalServerResourceAssignmentsFromProfileMsg
import org.zstack.kvm.KvmPhysicalServerAdapter
import org.zstack.kvm.KvmHostConfigChecker
import org.zstack.physicalserver.PhysicalServerCpuPlanner
import org.zstack.physicalserver.PhysicalServerResourceAssignmentGlobalConfig
import org.zstack.physicalserver.PhysicalServerResourceExtensionRegistry
import org.zstack.portal.managementnode.ManagementNodePhysicalServerAdapter
import org.zstack.storage.zbs.ZbsResourceUsageObserver

import java.net.URL
import java.net.URLClassLoader
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

class PhysicalServerResourceModelCase {
    @Test
    void testRoleTypeUsesTheRegisteredZStackTypePattern() {
        PhysicalServerRoleType type = registeredRoleType("UNIT_TEST_ROLE")
        assert PhysicalServerRoleType.valueOf("UNIT_TEST_ROLE").is(type)
        assertFailure("duplicate PhysicalServerRoleType") {
            new PhysicalServerRoleType("UNIT_TEST_ROLE")
        }
    }

    @Test
    void testResourceAssignmentDefaultPreservesUpgradeChoice() {
        def fallback = PhysicalServerResourceAssignmentGlobalConfig.class
                .getDeclaredField("ENABLED").getAnnotation(GlobalConfigDef.class)
        assert fallback.defaultValue() == "false" :
                "contexts without the dynamic detector must retain a linkable, safe-off default"

        Path legacy = Files.createTempDirectory("resource-assignment-v1-")
        Path unified = Files.createTempDirectory("resource-assignment-v2-")
        Files.createFile(unified.resolve("cgroup.controllers"))
        def method = PhysicalServerResourceAssignmentGlobalConfig.class
                .getDeclaredMethod("defaultEnabled", String.class, Path.class)
        method.accessible = true
        try {
            assert !method.invoke(null, null, legacy) :
                    "a fresh non-v2 environment must default resource assignment off"
            assert method.invoke(null, null, unified) :
                    "a fresh unified cgroup v2 environment must default resource assignment on"
            assert !method.invoke(null, "false", unified) :
                    "an upgrade-time false default must win over local cgroup v2 detection"
        } finally {
            Files.deleteIfExists(unified.resolve("cgroup.controllers"))
            Files.deleteIfExists(unified)
            Files.deleteIfExists(legacy)
        }
    }

    @Test
    void testKvmAgentBootstrapChecksConfigurationAndRuntimeMembership() {
        def unified = KvmHostConfigChecker.class.getDeclaredMethod(
                "unifiedResourceAssignmentMatches", String.class, String.class, String.class)
        unified.accessible = true
        def legacy = KvmHostConfigChecker.class.getDeclaredMethod(
                "legacyResourceAssignmentMatches", String.class, String.class)
        legacy.accessible = true

        assert unified.invoke(
                null, "true",
                "[Service]\nSlice=zstack-compute.slice", "/zstack.slice/zstack-compute.slice/zstack-kvmagent.service") :
                "enabled bootstrap requires both the exact drop-in and live Role membership"
        assert unified.invoke(
                null, "true",
                "[Service]\r\nSlice=zstack-compute.slice",
                "0::/zstack.slice/zstack-compute.slice/zstack-kvmagent.service") :
                "SSH line ending conversion must not cause repeated deployment"
        assert !unified.invoke(
                null, "true", "[Service]\nSlice=zstack-compute.slice", "/system.slice/zstack-kvmagent.service") :
                "a staged drop-in alone must not suppress the restart needed to join the Role"
        assert unified.invoke(null, "false", "__ABSENT__", "/system.slice/zstack-kvmagent.service") :
                "disabled bootstrap requires no managed drop-in and no live Role membership"
        assert !unified.invoke(
                null, "false", "__ABSENT__", "/zstack.slice/zstack-compute.slice/zstack-kvmagent.service") :
                "disabling must redeploy a KVM Agent that is still inside the Role slice"
        assert legacy.invoke(null, "true", "__ABSENT__") :
                "legacy cgroup hosts keep post-start assignment without a v2 bootstrap drop-in"
        assert legacy.invoke(null, "true", "[Service]\nSlice=zstack-compute.slice") :
                "legacy cgroup hosts let the shared Assignment Adapter own its drop-in"
        assert !legacy.invoke(null, "false", "[Service]\nSlice=zstack-compute.slice") :
                "disabling must remove a legacy Assignment Adapter drop-in"
    }

    @Test
    void testMachineSerialNormalizationRejectsFirmwarePlaceholders() {
        assert Platform.normalizeMachineSerialNumber("  PS-SN-01  ") == "ps-sn-01" :
                "machine identity must be stable across case and whitespace: " +
                        "expected=ps-sn-01 actual=${Platform.normalizeMachineSerialNumber('  PS-SN-01  ')}"
        [null, "", "N/A", "unknown", "To Be Filled", "default string"].each {
            assert Platform.normalizeMachineSerialNumber(it) == null :
                    "firmware placeholder must not create a PhysicalServer: " +
                            "serial=${it} actual=${Platform.normalizeMachineSerialNumber(it)}"
        }
    }

    @Test
    void testCpuSetNormalizesWithoutExpandingUntrustedRanges() {
        assert PhysicalServerCpuSet.normalize(" 3,1-2,2,5-6 ") == "1-3,5-6" :
                "CPUSet must merge and sort overlapping ranges: " +
                        "expected=1-3,5-6 actual=${PhysicalServerCpuSet.normalize(' 3,1-2,2,5-6 ')}"
        assert PhysicalServerCpuSet.count("1-3,5-6") == 5 :
                "CPUSet count must use normalized ranges: expected=5 " +
                        "actual=${PhysicalServerCpuSet.count('1-3,5-6')}"
        assert PhysicalServerCpuSet.union("0-1", "1-3") == "0-3" :
                "CPUSet union must merge adjacent ranges: expected=0-3 " +
                        "actual=${PhysicalServerCpuSet.union('0-1', '1-3')}"

        assertFailure("Invalid CPU id") {
            PhysicalServerCpuSet.normalize("1-")
        }
        assertFailure("is too large") {
            PhysicalServerCpuSet.parse("0-2147483647")
        }
        assertFailure("outside the online topology") {
            PhysicalServerCpuSet.parse("0-4", [0, 1, 2, 3] as Set<Integer>)
        }
    }

    @Test
    void testTopologyRequiresEveryOnlineCpuInExactlyOneCoreGroup() {
        PhysicalServerCpuTopology topology = topology()

        assert topology.onlineCpus == ([0, 1, 2, 3, 4, 5, 6, 7] as Set) :
                "topology must retain every online CPU exactly once: " + "expected=0-7 actual=${topology.onlineCpus}"
        def actualGroups = topology.coreGroups.collect {
            [it.numaId, it.cpus]
        }
        def expectedGroups = [["0", [0, 4] as Set], ["0", [1, 5] as Set], ["1", [2, 6] as Set], ["1", [3, 7] as Set],]
        assert actualGroups == expectedGroups :
                "topology fingerprint must be stable across NUMA map order: " +
                        "expected=${expectedGroups} actual=${actualGroups}"

        PhysicalServerNumaNode missingCpu = node("0", ["0", "1"], [["0"]])
        assertFailure("do not cover every online CPU") {
            PhysicalServerCpuTopology.from(["0": missingCpu])
        }

        PhysicalServerNumaNode duplicatedCpu = node("0", ["0", "1"], [["0"], ["0", "1"]])
        assertFailure("appears in multiple core groups") {
            PhysicalServerCpuTopology.from(["0": duplicatedCpu])
        }
    }

    @Test
    void testExclusiveCpuPlanningPreservesWholeCoresAndCpuZero() {
        PhysicalServerCpuPlanner planner = new PhysicalServerCpuPlanner()
        PhysicalServerCpuTopology topology = topology()

        String shared = planner.validateAndNormalize(
                PhysicalServerResourceIsolationMode.SHARED, "0-1,4-5", topology, [2, 3, 6, 7])
        assert shared == "0-1,4-5" :
                "shared CPUs outside exclusive reservations must remain valid: " + "expected=0-1,4-5 actual=${shared}"

        String exclusive = planner.validateAndNormalize(
                PhysicalServerResourceIsolationMode.EXCLUSIVE, "2-3,6-7", topology, Collections.emptySet())
        assert exclusive == "2-3,6-7" :
                "exclusive CPUs must accept complete SMT core groups: " + "expected=2-3,6-7 actual=${exclusive}"

        assertFailure("overlaps an exclusive role") {
            planner.validateAndNormalize(PhysicalServerResourceIsolationMode.SHARED, "0-2", topology, [2, 6])
        }
        assertFailure("splits core group") {
            planner.validateAndNormalize(
                    PhysicalServerResourceIsolationMode.EXCLUSIVE, "2", topology, Collections.emptySet())
        }
        assertFailure("CPU0 core group must remain shared") {
            planner.validateAndNormalize(
                    PhysicalServerResourceIsolationMode.EXCLUSIVE, "0,4", topology, Collections.emptySet())
        }
    }

    @Test
    void testProfileCpuCountResizesTheExistingAssignment() {
        PhysicalServerCpuPlanner planner = new PhysicalServerCpuPlanner()
        PhysicalServerCpuTopology topology = largeTopology()

        String unchanged = planner.matchProfileCpuCount(
                4, "1-4", PhysicalServerResourceIsolationMode.SHARED, topology, Collections.emptySet())
        assert unchanged == "1-4" :
                "matching Profile count must preserve the assigned CPUs"

        String expanded = planner.matchProfileCpuCount(
                6, unchanged, PhysicalServerResourceIsolationMode.SHARED, topology, Collections.emptySet())
        assert PhysicalServerCpuSet.parse(expanded).containsAll(PhysicalServerCpuSet.parse(unchanged)) :
                "Profile expansion must add CPUs to the existing assignment: " + "before=${unchanged} after=${expanded}"
        assert PhysicalServerCpuSet.count(expanded) == 6

        String shrunk = planner.matchProfileCpuCount(
                2, expanded, PhysicalServerResourceIsolationMode.SHARED, topology, Collections.emptySet())
        assert PhysicalServerCpuSet.parse(expanded).containsAll(PhysicalServerCpuSet.parse(shrunk)) :
                "Profile shrink must remove CPUs from the existing assignment: " + "before=${expanded} after=${shrunk}"
        assert PhysicalServerCpuSet.count(shrunk) == 2

        assert planner.matchProfileCpuCount(
                null,
                expanded, PhysicalServerResourceIsolationMode.SHARED, topology, Collections.emptySet()) == expanded :
                "removing defaultCpuCount must not clear an existing CPU assignment"
    }

    @Test
    void testExclusiveProfileCpuCountUsesCompleteCoresOnOneNuma() {
        PhysicalServerCpuPlanner planner = new PhysicalServerCpuPlanner()
        PhysicalServerCpuTopology topology = topology()

        String initial = planner.matchProfileCpuCount(
                4, "", PhysicalServerResourceIsolationMode.EXCLUSIVE, topology, Collections.emptySet())
        assert initial == "2-3,6-7" :
                "an exclusive Profile must choose complete cores from one NUMA node: " +
                        "expected=2-3,6-7 actual=${initial}"

        String shrunk = planner.matchProfileCpuCount(
                2, initial, PhysicalServerResourceIsolationMode.EXCLUSIVE, topology, Collections.emptySet())
        assert shrunk == "2,6" :
                "exclusive shrink must remove a complete core: " + "expected=2,6 actual=${shrunk}"

        assert planner.matchProfileCpuCount(
                4, shrunk, PhysicalServerResourceIsolationMode.EXCLUSIVE, topology, Collections.emptySet()) == initial :
                "exclusive expansion must restore a complete core on the same NUMA node"

        assertFailure("cannot be allocated as complete cores") {
            planner.matchProfileCpuCount(
                    3, initial, PhysicalServerResourceIsolationMode.EXCLUSIVE, topology, Collections.emptySet())
        }
    }

    @Test
    void testManagementAndComputeDefaultsUseEightAvailableCpus() {
        PhysicalServerCpuTopology large = largeTopology()
        Set<Integer> exclusive = [8, 9, 18, 19] as Set<Integer>
        PhysicalServerCpuPlanner planner = new PhysicalServerCpuPlanner()
        KvmPhysicalServerAdapter kvm = new KvmPhysicalServerAdapter()
        ManagementNodePhysicalServerAdapter mn = new ManagementNodePhysicalServerAdapter()

        assert kvm.defaultCpuCount == 8 :
                "COMPUTE Profile must expose its CPU count without choosing CPUs"
        assert mn.defaultCpuCount == 8 :
                "MANAGEMENT Profile must expose its CPU count without choosing CPUs"
        String compute = planner.calculateDefaultCpuSet(kvm.defaultCpuCount, large, exclusive)
        String management = planner.calculateDefaultCpuSet(mn.defaultCpuCount, large, exclusive)

        assert compute == "1-7,11" :
                "compute default must select eight logical CPUs without the CPU0 CoreGroup: " +
                        "expected=1-7,11 actual=${compute}"
        assert management == compute :
                "management and compute shared Roles must use the same default: " +
                        "expected=${compute} actual=${management}"
        assert Collections.disjoint(PhysicalServerCpuSet.parse(compute), exclusive) :
                "shared defaults must exclude CPUs reserved by Exclusive Roles: " +
                        "cpuSet=${compute} exclusive=${exclusive}"
        assert Collections.disjoint(PhysicalServerCpuSet.parse(compute), large.cpuZeroGroup.cpus) :
                "automatic shared defaults must exclude every SMT sibling of CPU0: " +
                        "cpuSet=${compute} cpuZeroGroup=${large.cpuZeroGroup.cpus}"

        String constrained = PhysicalServerCpuSet
                .firstAvailableExcludingCpuZeroCore(topology(), [2, 3, 6, 7] as Set<Integer>, 2)
        assert constrained == "1,5" :
                "a constrained default must still reserve the complete CPU0 CoreGroup: " +
                        "expected=1,5 actual=${constrained}"

        String secondNuma = PhysicalServerCpuSet
                .firstAvailableExcludingCpuZeroCore(topology(), Collections.emptySet(), 4)
        assert secondNuma == "2-3,6-7" :
                "automatic CPU allocation must stay inside one NUMA node: " + "expected=2-3,6-7 actual=${secondNuma}"
        String crossNuma = PhysicalServerCpuSet
                .firstAvailableExcludingCpuZeroCore(topology(), Collections.emptySet(), 5)
        assert crossNuma == "1-3,5-6" :
                "automatic CPU allocation must fall back across NUMA nodes when one node cannot " +
                        "satisfy the requested count: " +
                        "expected=1-3,5-6 actual=${crossNuma}"

        String limited = planner.matchProfileCpuCount(
                8, "1-3,5-7", PhysicalServerResourceIsolationMode.SHARED, topology(), Collections.emptySet())
        assert limited == "1-3,5-7" :
                "refresh must keep all available shared CPUs when hardware cannot satisfy the Profile count: " +
                        "expected=1-3,5-7 actual=${limited}"
    }

    @Test
    void testRoleProfilesReloadOnlyWhenExplicitlyRequested() {
        Path root = Files.createTempDirectory("resource-assignment-profile-")
        Path profiles = Files.createDirectories(root.resolve("physical-server-roles"))
        Path compute = profiles.resolve("compute.yaml")
        Path management = profiles.resolve("management.yaml")
        Path zbs = profiles.resolve("zbs.yaml")
        ClassLoader previous = Thread.currentThread().contextClassLoader
        URLClassLoader loader = new URLClassLoader([root.toUri().toURL()] as URL[], (ClassLoader) null)
        try {
            Thread.currentThread().contextClassLoader = loader
            Files.write(compute, controlledRoleManifest("COMPUTE", "zstack-compute.slice", 2)
                    .getBytes(StandardCharsets.UTF_8))
            Files.write(management, controlledRoleManifest("MANAGEMENT", "zstack-management.slice", 3)
                    .getBytes(StandardCharsets.UTF_8))
            Files.write(zbs, observedRoleManifest("first.slice", PhysicalServerResourceIsolationMode.EXCLUSIVE)
                    .getBytes(StandardCharsets.UTF_8))
            RoleServiceManifest.reloadAll()

            PhysicalServerCpuTopology topology = largeTopology()
            KvmPhysicalServerAdapter kvm = new KvmPhysicalServerAdapter()
            ManagementNodePhysicalServerAdapter mn = new ManagementNodePhysicalServerAdapter()
            ZbsResourceUsageObserver zbsObserver = new ZbsResourceUsageObserver()
            assert defaultCpuSet(kvm, topology) == "1-2" :
                    "compute must read the current external profile without a management-node restart"
            assert defaultCpuSet(mn, topology) == "1-3" :
                    "management must read the current external profile without a management-node restart"
            assert observedZbsServices(zbsObserver) == ["first.slice"] :
                    "ZBS observation must read the current external profile without a management-node restart"
            assert zbsObserver.isolationMode == PhysicalServerResourceIsolationMode.EXCLUSIVE :
                    "ZBS must read its Exclusive reservation policy from the external profile"

            Files.write(compute, controlledRoleManifest("COMPUTE", "zstack-compute.slice", 4)
                    .getBytes(StandardCharsets.UTF_8))
            Files.write(management, controlledRoleManifest("MANAGEMENT", "zstack-management.slice", 5)
                    .getBytes(StandardCharsets.UTF_8))
            Files.write(zbs, observedRoleManifest("second.slice", PhysicalServerResourceIsolationMode.SHARED)
                    .getBytes(StandardCharsets.UTF_8))
            assert defaultCpuSet(kvm, topology) == "1-2" :
                    "editing a Profile must not change COMPUTE before the explicit reload API"
            assert defaultCpuSet(mn, topology) == "1-3" :
                    "editing a Profile must not change MANAGEMENT before the explicit reload API"
            assert observedZbsServices(zbsObserver) == ["first.slice"] :
                    "editing a Profile must not change ZBS observation before the explicit reload API"
            assert zbsObserver.isolationMode == PhysicalServerResourceIsolationMode.EXCLUSIVE :
                    "editing a Profile must not change ZBS isolation before the explicit reload API"

            RoleServiceManifest.reloadAll()
            assert defaultCpuSet(kvm, topology) == "1-4" :
                    "the explicit reload API must activate the changed COMPUTE Profile"
            assert defaultCpuSet(mn, topology) == "1-5" :
                    "the explicit reload API must activate the changed MANAGEMENT Profile"
            assert observedZbsServices(zbsObserver) == ["second.slice"] :
                    "the explicit reload API must activate the changed ZBS Profile"
            assert zbsObserver.isolationMode == PhysicalServerResourceIsolationMode.SHARED :
                    "the explicit reload API must activate the changed ZBS isolation policy"
        } finally {
            Thread.currentThread().contextClassLoader = previous
            RoleServiceManifest.reloadAll()
            loader.close()
            Files.deleteIfExists(compute)
            Files.deleteIfExists(management)
            Files.deleteIfExists(zbs)
            Files.deleteIfExists(profiles)
            Files.deleteIfExists(root)
        }
    }

    @Test
    void testProfileRefreshApiSupportsScopedBatchAndGlobalRefresh() {
        RestRequest request = APIRefreshPhysicalServerResourceAssignmentsFromProfileMsg.getAnnotation(RestRequest.class)
        assert request.path() == "/physical-servers/resource-assignments/actions" :
                "Profile reload must use the global Refresh path: actual=${request.path()}"
        assert request.optionalPaths().length == 0 :
                "Refresh scope must be expressed only by serverUuids in the request body: " +
                        "expected=[] actual=${request.optionalPaths()}"

        APIParam serverUuids = APIRefreshPhysicalServerResourceAssignmentsFromProfileMsg
                .getDeclaredField("serverUuids").getAnnotation(APIParam.class)
        assert !serverUuids.required() && serverUuids.nonempty() :
                "serverUuids must distinguish omitted global scope from an invalid empty scope: " +
                        "required=${serverUuids.required()} nonempty=${serverUuids.nonempty()}"
    }

    @Test
    void testRoleManifestsDefineStableHandlesWithoutAdapterCodeChanges() {
        RoleServiceManifest compute = RoleServiceManifest.load("physical-server-roles/compute.yaml", "COMPUTE")
        List<ResourceConsumerHandle> computeHandles = compute.handles()

        assert compute.sliceName == "zstack-compute.slice" :
                "compute manifest must preserve its stable Role slice: " +
                        "expected=zstack-compute.slice actual=${compute.sliceName}"
        assert compute.defaultCpuCount == 8 :
                "compute manifest must make the 8C default explicit: " + "expected=8 actual=${compute.defaultCpuCount}"
        assert compute.isolationMode == PhysicalServerResourceIsolationMode.SHARED :
                "compute isolation policy must come from its Role manifest: " +
                        "expected=SHARED actual=${compute.isolationMode}"
        assert computeHandles*.serviceName.containsAll([
                "kvmagent", "virtlogd", "network-agent", "sharedblock-agent", "node-exporter"]) :
                "a Role manifest must generate its complete stable service set: " +
                        "actual=${computeHandles*.serviceName}"
        assert computeHandles.find { it.serviceName == "node-exporter" }.restartable :
                "manifest restartability must reach the generated handle: " +
                        "service=node-exporter " +
                        "actual=${computeHandles.find { it.serviceName == 'node-exporter' }.restartable}"

        RoleServiceManifest zbs = RoleServiceManifest.loadObservation("physical-server-roles/zbs.yaml", "ZBS")
        assert zbs.services*.name == ["zstone.share.slice", "zstone.cs.slice", "zstone.vhost.slice"] :
                "ZBS observation must expose the canonical ZStone Slice names without allocation defaults: " +
                        "actual=${zbs.services*.name}"
        assert zbs.isolationMode == PhysicalServerResourceIsolationMode.EXCLUSIVE :
                "ZBS observed CPUs must be reserved from Shared Roles: " +
                        "expected=EXCLUSIVE actual=${zbs.isolationMode}"
        assert zbs.sliceName == null && zbs.defaultCpuCount == null :
                "an observation-only ZBS manifest must not define an allocation plan"
        assertFailure("does not match expected roleType") {
            RoleServiceManifest.load("physical-server-roles/compute.yaml", "IMAGE_STORE")
        }
        assertFailure("observation-only role cannot define allocation defaults") {
            RoleServiceManifest.loadObservation("physical-server-roles/invalid-provider-slice.yaml", "INVALID_PROVIDER")
        }
    }

    @Test
    void testExtensionRegistryKeepsPeerRolesIndependentAndRejectsAmbiguity() {
        PhysicalServerResourceAssignmentController compute = adapter("COMPUTE")
        PhysicalServerResourceAssignmentController imageStore = adapter("IMAGE_STORE")
        PhysicalServerResourceExtensionRegistry loaded =
                PhysicalServerResourceExtensionRegistry.load(registry(
                        [imageStore], [], [], [association("IMAGE_STORE")]))
        assert loaded.controller("IMAGE_STORE").is(imageStore) :
                "a peer Role controller must not depend on another Role as its topology source"

        PluginRegistry pluginRegistry = registry(
                [compute, imageStore], [], [], [association("COMPUTE"), association("IMAGE_STORE")])

        loaded = PhysicalServerResourceExtensionRegistry.load(pluginRegistry)
        assert loaded.orderedControllers().collect {
            it.roleType.toString()
        } ==
                ["COMPUTE", "IMAGE_STORE"] :
                "valid controllers must be ordered without enum registration: " +
                        "actual=${loaded.orderedControllers()*.roleType}"

        assertFailure("duplicate PhysicalServer Role extensions") {
            PhysicalServerResourceExtensionRegistry.load(registry(
                    [compute,
                     adapter("IMAGE_STORE"),
                     adapter("IMAGE_STORE")], [], [], [association("COMPUTE"), association("IMAGE_STORE")]))
        }

        assertFailure("no PhysicalServer Role association provider") {
            PhysicalServerResourceExtensionRegistry.load(registry(
                    [compute, adapter("IMAGE_STORE")], [], [], [association("COMPUTE")]))
        }
    }

    @Test
    void testExtensionRegistryKeepsReadWriteAndUsageCapabilitiesIndependent() {
        PhysicalServerResourceAssignmentController compute = adapter("COMPUTE")
        PhysicalServerResourceAssignmentObserver zbsAssignment =
                assignmentObserver("ZBS", PhysicalServerResourceIsolationMode.EXCLUSIVE)
        PhysicalServerResourceUsageObserver zbs = observer("ZBS")
        PluginRegistry pluginRegistry = registry(
                [compute], [zbsAssignment], [observer("COMPUTE"), zbs], [association("COMPUTE"), association("ZBS")])

        PhysicalServerResourceExtensionRegistry extensions =
                PhysicalServerResourceExtensionRegistry.load(pluginRegistry)

        assert extensions.orderedControllers().collect {
            it.roleType.toString()
        } == ["COMPUTE"] :
                "an observation-only ZBS integration must not enter the executable Assignment registry: " +
                        "actual=${extensions.orderedControllers()*.roleType}"
        assert extensions.orderedReadOnlyObservers().collect {
            it.roleType.toString()
        } == ["ZBS"] :
                "read-only Assignment must remain separate from writable controllers"
        assert extensions.observer("ZBS").isolationMode == PhysicalServerResourceIsolationMode.EXCLUSIVE :
                "read-only Assignments must expose the isolation policy used by conflict checks"
        assert extensions.observer("COMPUTE").is(compute) :
                "a writable controller must also satisfy the shared read contract"
        assert extensions.usageObserver("COMPUTE") != null :
                "controlled Role must expose its usage observer"
        assert extensions.usageObserver("ZBS") != null :
                "read-only Role must expose its usage observer independently"

        assertFailure("duplicate PhysicalServer Role extensions") {
            PhysicalServerResourceExtensionRegistry.load(registry(
                        [compute], [], [observer("ZBS"), observer("ZBS")], [association("COMPUTE")]))
        }
    }

    private static PhysicalServerCpuTopology topology() {
        return PhysicalServerCpuTopology.from([
                "1": node("1", ["2", "3", "6", "7"],
                        [["2", "6"], ["3", "7"]]), "0": node("0", ["0", "1", "4", "5"], [["0", "4"], ["1", "5"]])])
    }

    private static PhysicalServerCpuTopology largeTopology() {
        return PhysicalServerCpuTopology.from([
                "0": node(
                        "0",
                        (0..19).collect { it.toString() },
                        (0..9).collect { [it.toString(), (it + 10).toString()] })
        ])
    }

    private static String defaultCpuSet(
            PhysicalServerResourceAssignmentController controller, PhysicalServerCpuTopology topology) {
        return new PhysicalServerCpuPlanner().calculateDefaultCpuSet(
                controller.defaultCpuCount, topology, Collections.emptySet())
    }

    private static PhysicalServerNumaNode node(String nodeId, List<String> online, List<List<String>> coreGroups) {
        PhysicalServerNumaNode node = new PhysicalServerNumaNode()
        node.nodeId = nodeId
        node.onlineCpus = online
        node.coreGroups = coreGroups
        return node
    }

    private static String controlledRoleManifest(String roleType, String sliceName, int defaultCpuCount) {
        return """roleType: ${roleType}
isolationMode: SHARED
sliceName: ${sliceName}
defaultCpuCount: ${defaultCpuCount}
services:
  - name: test-service
    handleType: SYSTEMD_UNIT
    value: test.service
    required: true
    restartable: false
"""
    }

    private static String observedRoleManifest(String serviceName, PhysicalServerResourceIsolationMode isolationMode) {
        return """roleType: ZBS
isolationMode: ${isolationMode}
services:
  - name: ${serviceName}
"""
    }

    private static List<String> observedZbsServices(ZbsResourceUsageObserver observer) {
        return RoleServiceManifest.loadObservation(
                ZbsResourceUsageObserver.ROLE_SERVICE_MANIFEST_PATH,
                observer.roleType.toString()).managedServiceUsages("NOT_FOUND")*.serviceName
    }

    private static PhysicalServerResourceAssignmentController adapter(String roleType) {
        PhysicalServerResourceAssignmentController adapter = mock(PhysicalServerResourceAssignmentController.class)
        when(adapter.getRoleType()).thenReturn(registeredRoleType(roleType))
        when(adapter.getIsolationMode()).thenReturn(PhysicalServerResourceIsolationMode.SHARED)
        return adapter
    }

    private static PhysicalServerResourceAssignmentObserver assignmentObserver(
            String roleType, PhysicalServerResourceIsolationMode isolationMode) {
        PhysicalServerResourceAssignmentObserver observer = mock(PhysicalServerResourceAssignmentObserver.class)
        when(observer.getRoleType()).thenReturn(registeredRoleType(roleType))
        when(observer.getIsolationMode()).thenReturn(isolationMode)
        return observer
    }

    private static PhysicalServerRoleAssociationProvider association(String roleType) {
        PhysicalServerRoleAssociationProvider provider = mock(PhysicalServerRoleAssociationProvider.class)
        when(provider.getRoleType()).thenReturn(registeredRoleType(roleType))
        return provider
    }

    private static PhysicalServerResourceUsageObserver observer(String roleType) {
        PhysicalServerResourceUsageObserver observer = mock(PhysicalServerResourceUsageObserver.class)
        when(observer.getRoleType()).thenReturn(registeredRoleType(roleType))
        return observer
    }

    private static PhysicalServerRoleType registeredRoleType(String typeName) {
        if (KvmPhysicalServerAdapter.type.toString() == typeName) {
            return KvmPhysicalServerAdapter.type
        }
        if (ManagementNodePhysicalServerAdapter.type.toString() == typeName) {
            return ManagementNodePhysicalServerAdapter.type
        }
        if (ZbsResourceUsageObserver.type.toString() == typeName) {
            return ZbsResourceUsageObserver.type
        }
        try {
            return PhysicalServerRoleType.valueOf(typeName)
        } catch (IllegalArgumentException ignored) {
            return new PhysicalServerRoleType(typeName)
        }
    }

    private static PluginRegistry registry(
            List<PhysicalServerResourceAssignmentController> controllers,
            List<PhysicalServerResourceAssignmentObserver> assignmentObservers,
            List<PhysicalServerResourceUsageObserver> usageObservers,
            List<PhysicalServerRoleAssociationProvider> associations) {
        PluginRegistry registry = mock(PluginRegistry.class)
        when(registry.getExtensionList(PhysicalServerResourceAssignmentController.class)).thenReturn(controllers)
        when(registry.getExtensionList(PhysicalServerResourceAssignmentObserver.class)).thenReturn(assignmentObservers)
        when(registry.getExtensionList(PhysicalServerResourceUsageObserver.class)).thenReturn(usageObservers)
        when(registry.getExtensionList(PhysicalServerRoleAssociationProvider.class)).thenReturn(associations)
        return registry
    }

    private static void assertFailure(String expectedMessage, Closure operation) {
        Throwable failure = null
        try {
            operation.call()
        } catch (Throwable error) {
            failure = error
        }
        assert failure != null :
                "operation must fail with a typed validation error: " +
                        "expectedMessage=${expectedMessage} actual=no failure"
        assert failure.message?.contains(expectedMessage) :
                "validation failure must expose the expected reason: " +
                        "expectedMessage=${expectedMessage} " +
                        "actualType=${failure.class.name} actualMessage=${failure.message}"
    }
}
