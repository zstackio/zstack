package org.zstack.test.unittest.physicalserver

import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
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
import org.zstack.header.physicalserver.PhysicalServerResourceAssignmentFactory
import org.zstack.physicalserver.PhysicalServerManagerImpl
import org.zstack.header.physicalserver.PhysicalServerResourceBoundary
import org.zstack.header.core.ReturnValueCompletion
import org.zstack.header.physicalserver.PhysicalServerRoleType
import org.zstack.header.physicalserver.ResourceConsumerHandle
import org.zstack.header.physicalserver.RoleServiceManifest
import org.zstack.header.rest.RestRequest
import org.zstack.physicalserver.APIRefreshPhysicalServerResourceAssignmentsFromProfileMsg
import org.zstack.kvm.KvmResourceAssignmentFactory
import org.zstack.kvm.KvmResourceAssignmentController
import org.zstack.kvm.KvmHostConfigChecker
import org.zstack.physicalserver.PhysicalServerCpuPlanner
import org.zstack.physicalserver.PhysicalServerResourceAssignmentGlobalConfig
import org.zstack.portal.managementnode.ManagementNodeResourceAssignmentFactory
import org.zstack.portal.managementnode.ManagementNodeResourceAssignmentController
import org.zstack.storage.zbs.ZbsResourceAssignmentFactory
import org.zstack.storage.zbs.ZbsResourceAssignmentObserver

import java.net.URL
import java.net.URLClassLoader
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

import static groovy.test.GroovyAssert.shouldFail
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

class PhysicalServerResourceModelCase {
    private static final Map<String, PhysicalServerRoleType> testRoleTypes = [:]
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder()
    private ClassLoader previousContextClassLoader
    private URLClassLoader profileClassLoader

    @After
    void restoreRoleProfiles() {
        if (previousContextClassLoader == null) {
            return
        }
        Thread.currentThread().contextClassLoader = previousContextClassLoader
        profileClassLoader.close()
        previousContextClassLoader = null
        profileClassLoader = null
        RoleServiceManifest.reloadAll()
    }

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

        Path legacy = temporaryFolder.newFolder("resource-assignment-v1").toPath()
        Path unified = temporaryFolder.newFolder("resource-assignment-v2").toPath()
        Files.createFile(unified.resolve("cgroup.controllers"))
        def method = PhysicalServerResourceAssignmentGlobalConfig.class
                .getDeclaredMethod("defaultEnabled", String.class, Path.class)
        method.accessible = true
        assert !method.invoke(null, null, legacy) :
                "a fresh non-v2 environment must default resource assignment off"
        assert method.invoke(null, null, unified) :
                "a fresh unified cgroup v2 environment must default resource assignment on"
        assert !method.invoke(null, "false", unified) :
                "an upgrade-time false default must win over local cgroup v2 detection"
    }

    @Test
    void testKvmAgentBootstrapChecksConfigurationAndRuntimeMembership() {
        def needDeploy = KvmHostConfigChecker.class.getDeclaredMethod("needDeployResourceAssignment")
        needDeploy.accessible = true
        def unified = KvmHostConfigChecker.class.getDeclaredMethod(
                "unifiedResourceAssignmentMatches", String.class, String.class, String.class)
        unified.accessible = true
        def legacy = KvmHostConfigChecker.class.getDeclaredMethod("legacyResourceAssignmentMatches", String.class)
        legacy.accessible = true

        assert !needDeploy.invoke(new KvmHostConfigChecker()) :
                "disabled bootstrap must not inspect or modify an existing Role membership"
        assert unified.invoke(null,
                "[Service]\nSlice=zstack-compute.slice", "/zstack.slice/zstack-compute.slice/zstack-kvmagent.service",
                "zstack-compute.slice") :
                "enabled bootstrap requires both the exact drop-in and live Role membership"
        assert unified.invoke(null, "[Service]\r\nSlice=zstack-compute.slice",
                "0::/zstack.slice/zstack-compute.slice/zstack-kvmagent.service", "zstack-compute.slice") :
                "SSH line ending conversion must not cause repeated deployment"
        assert !unified.invoke(null, "[Service]\nSlice=zstack-compute.slice", "/system.slice/zstack-kvmagent.service",
                "zstack-compute.slice") :
                "a staged drop-in alone must not suppress the restart needed to join the Role"
        assert !legacy.invoke(null, "__ABSENT__") :
                "enabled legacy hosts must receive install-time Role membership"
        assert legacy.invoke(null, "[Service]\nSlice=zstack-compute.slice") :
                "enabled legacy hosts require the exact Role drop-in"
        assert unified.invoke(null, "# generated\r\n[Service]\r\n\n Slice = custom:role.slice\n",
                "0::/custom:role.slice/zstack-kvmagent.service", "custom:role.slice") :
                "custom initial slices and semantically equivalent drop-ins must not trigger redeployment"
        assert unified.invoke(null, "[Service]\nSlice=zstack-management.slice",
                "/zstack.slice/zstack-management.slice/zstack-kvmagent.service", "zstack-compute.slice") :
                "the first Role to own a service must retain ownership"
        assert !legacy.invoke(null, "[Service]\nSlice=../../invalid.slice") : "invalid slice paths must be rejected"
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

        assert planner.matchProfileCpuCount(null,
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
    void testInitialAllocationAndExpansionUseTheSameCpuSetGrowth() {
        PhysicalServerCpuPlanner planner = new PhysicalServerCpuPlanner()
        PhysicalServerCpuTopology topology = topology()
        for (PhysicalServerResourceIsolationMode mode : PhysicalServerResourceIsolationMode.values()) {
            String initial = planner.matchProfileCpuCount(6, "", mode, topology, Collections.emptySet())
            String existing = planner.matchProfileCpuCount(2, "", mode, topology, Collections.emptySet())
            String expanded = planner.matchProfileCpuCount(6, existing, mode, topology, Collections.emptySet())
            assert initial == "1-3,5-7" && expanded == initial :
                    "initial and incremental allocation must both span NUMA when needed: " +
                            "mode=${mode} initial=${initial} expanded=${expanded}"
            assert PhysicalServerCpuSet.parse(expanded).containsAll(PhysicalServerCpuSet.parse(existing)) :
                    "growth must retain the existing CPUs: before=${existing} after=${expanded}"
        }
    }

    @Test
    void testManagementAndComputeDefaultsUseEightAvailableCpus() {
        PhysicalServerCpuTopology large = largeTopology()
        Set<Integer> exclusive = [8, 9, 18, 19] as Set<Integer>
        PhysicalServerCpuPlanner planner = new PhysicalServerCpuPlanner()
        KvmResourceAssignmentFactory kvm = new KvmResourceAssignmentFactory()
        ManagementNodeResourceAssignmentFactory mn = new ManagementNodeResourceAssignmentFactory()

        assert kvm.roleServices().defaultCpuCount == 8 :
                "COMPUTE Profile must expose its CPU count without choosing CPUs"
        assert mn.roleServices().defaultCpuCount == 8 :
                "MANAGEMENT Profile must expose its CPU count without choosing CPUs"
        String compute = planner.matchProfileCpuCount(
                kvm.roleServices().defaultCpuCount, "", PhysicalServerResourceIsolationMode.SHARED, large, exclusive)
        String management = planner.matchProfileCpuCount(
                mn.roleServices().defaultCpuCount, "", PhysicalServerResourceIsolationMode.SHARED, large, exclusive)

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

        String constrained = planner.matchProfileCpuCount(
                2, "", PhysicalServerResourceIsolationMode.SHARED, topology(), [2, 3, 6, 7] as Set<Integer>)
        assert constrained == "1,5" :
                "a constrained default must still reserve the complete CPU0 CoreGroup: " +
                        "expected=1,5 actual=${constrained}"

        String secondNuma = planner.matchProfileCpuCount(
                4, "", PhysicalServerResourceIsolationMode.SHARED, topology(), Collections.emptySet())
        assert secondNuma == "2-3,6-7" :
                "automatic CPU allocation must stay inside one NUMA node: " + "expected=2-3,6-7 actual=${secondNuma}"
        String crossNuma = planner.matchProfileCpuCount(
                5, "", PhysicalServerResourceIsolationMode.SHARED, topology(), Collections.emptySet())
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
        Path root = temporaryFolder.newFolder("resource-assignment-profile").toPath()
        Path profiles = Files.createDirectories(root.resolve("physical-server-roles"))
        Path compute = profiles.resolve("compute.yaml")
        Path management = profiles.resolve("management.yaml")
        Path zbs = profiles.resolve("zbs.yaml")
        previousContextClassLoader = Thread.currentThread().contextClassLoader
        profileClassLoader = new URLClassLoader([root.toUri().toURL()] as URL[], (ClassLoader) null)
        Thread.currentThread().contextClassLoader = profileClassLoader
        Files.write(compute, controlledRoleManifest("COMPUTE", "zstack-compute.slice", 2)
                .getBytes(StandardCharsets.UTF_8))
        Files.write(management, controlledRoleManifest("MANAGEMENT", "zstack-management.slice", 3)
                .getBytes(StandardCharsets.UTF_8))
        Files.write(zbs, observedRoleManifest("first.slice", PhysicalServerResourceIsolationMode.EXCLUSIVE)
                .getBytes(StandardCharsets.UTF_8))
        RoleServiceManifest.reloadAll()

        PhysicalServerCpuTopology topology = largeTopology()
        KvmResourceAssignmentFactory kvm = new KvmResourceAssignmentFactory()
        ManagementNodeResourceAssignmentFactory mn = new ManagementNodeResourceAssignmentFactory()
        ZbsResourceAssignmentFactory zbsObserver = new ZbsResourceAssignmentFactory()
        assert defaultCpuSet(kvm, topology) == "1-2" :
                "compute must read the current external profile without a management-node restart"
        assert defaultCpuSet(mn, topology) == "1-3" :
                "management must read the current external profile without a management-node restart"
        assert observedZbsServices(zbsObserver) == ["first.slice"] :
                "ZBS observation must read the current external profile without a management-node restart"
        assert zbsObserver.roleServices().isolationMode == PhysicalServerResourceIsolationMode.EXCLUSIVE :
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
        assert zbsObserver.roleServices().isolationMode == PhysicalServerResourceIsolationMode.EXCLUSIVE :
                "editing a Profile must not change ZBS isolation before the explicit reload API"

        RoleServiceManifest.reloadAll()
        assert defaultCpuSet(kvm, topology) == "1-4" :
                "the explicit reload API must activate the changed COMPUTE Profile"
        assert defaultCpuSet(mn, topology) == "1-5" :
                "the explicit reload API must activate the changed MANAGEMENT Profile"
        assert observedZbsServices(zbsObserver) == ["second.slice"] :
                "the explicit reload API must activate the changed ZBS Profile"
        assert zbsObserver.roleServices().isolationMode == PhysicalServerResourceIsolationMode.SHARED :
                "the explicit reload API must activate the changed ZBS isolation policy"

        [0L, 1048576L].each { memory ->
            Files.write(compute, (controlledRoleManifest("COMPUTE", "zstack-compute.slice", 4) +
                    "\ndefaultMemory: ${memory}\n").getBytes(StandardCharsets.UTF_8))
            RoleServiceManifest.reloadAll()
            assert kvm.roleServices().defaultMemory == memory :
                    "zero and whole MiB defaults must be accepted: actual=${kvm.roleServices().defaultMemory}"
        }
        [-1L, 1L].each { memory ->
            Files.write(compute, (controlledRoleManifest("COMPUTE", "zstack-compute.slice", 6) +
                    "\ndefaultMemory: ${memory}\n").getBytes(StandardCharsets.UTF_8))
            shouldFail(IllegalStateException) { RoleServiceManifest.reloadAll() }
            assert kvm.roleServices().defaultMemory == 1048576L && kvm.roleServices().defaultCpuCount == 4 :
                    "invalid memory must reject the entire reload and preserve the last valid snapshot"
        }
    }

    @Test
    void testExplicitReloadBypassesClassLoaderContentCache() {
        Path root = temporaryFolder.newFolder("cached-profile").toPath()
        Path profiles = Files.createDirectories(root.resolve("physical-server-roles"))
        Path compute = profiles.resolve("compute.yaml")
        Files.write(compute, controlledRoleManifest("COMPUTE", "zstack-compute.slice", 1).getBytes("UTF-8"))
        previousContextClassLoader = Thread.currentThread().contextClassLoader
        profileClassLoader = new URLClassLoader([root.toUri().toURL()] as URL[], new ClassLoader(null) {}) {
            private final Map<String, byte[]> content = [:]

            @Override
            InputStream getResourceAsStream(String name) {
                if (!content.containsKey(name)) {
                    InputStream stream = super.getResourceAsStream(name)
                    if (stream == null) {
                        return null
                    }
                    content[name] = stream.withCloseable { it.bytes }
                }
                return new ByteArrayInputStream(content[name])
            }
        }
        Thread.currentThread().contextClassLoader = profileClassLoader
        RoleServiceManifest.reloadAll()
        def manifest = { RoleServiceManifest.load("physical-server-roles/compute.yaml", "COMPUTE") }
        assert manifest().defaultCpuCount == 1 : "the initial external Profile must allocate one CPU"

        [3, 2, 4].each { int count ->
            int previous = manifest().defaultCpuCount
            Files.write(compute, controlledRoleManifest("COMPUTE", "zstack-compute.slice", count).getBytes("UTF-8"))
            assert manifest().defaultCpuCount == previous : "editing YAML alone must not reload the Profile"
            RoleServiceManifest.reloadAll()
            assert manifest().defaultCpuCount == count :
                    "explicit reload must bypass cached content: expected=${count} actual=${manifest().defaultCpuCount}"
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
            RoleServiceManifest.load("physical-server-roles/compute.yaml", "TEST_SHARED_STORAGE")
        }
        assertFailure("observation-only role cannot define allocation defaults") {
            RoleServiceManifest.loadObservation("physical-server-roles/invalid-provider-slice.yaml", "INVALID_PROVIDER")
        }
    }

    @Test
    void testFactoriesCreateControllersWithoutRoleBaseOrMessageHandlers() {
        PhysicalServerRoleType role = registeredRoleType("TEST_SHARED_STORAGE")
        PhysicalServerResourceAssignmentFactory factory = [
                getRoleType: { role },
                roleServices: { new RoleServiceManifest(roleType: role.toString(), sliceName: "zstack-store.slice") },
                getResourceAssignment: { String serverUuid -> new TestController(serverUuid, role) }
        ] as PhysicalServerResourceAssignmentFactory
        PluginRegistry plugins = mock(PluginRegistry.class)
        when(plugins.getExtensionList(PhysicalServerResourceAssignmentFactory.class)).thenReturn([factory])
        PhysicalServerManagerImpl manager = new PhysicalServerManagerImpl()
        manager.@pluginRgty = plugins
        assert manager.getFactory(role.toString()).is(factory) : "Manager must select the factory by roleType"
        def first = factory.getResourceAssignment("server-1")
        def second = factory.getResourceAssignment("server-2")
        assert !first.is(second) : "different servers must not share a Controller instance"
        assert first.serverUuid == "server-1" && second.serverUuid == "server-2" :
                "each Controller must be bound to the requested server without an Assignment VO"
        assert first.class.superclass == Object : "external Controllers must not inherit a physicalServer Base"
        assert !PhysicalServerResourceAssignmentObserver.methods.any { it.name == "handleMessage" } :
                "message handling belongs to physicalServer, not the Observer contract"
        assert PhysicalServerResourceAssignmentFactory.getMethod("getResourceAssignment", String).returnType ==
                PhysicalServerResourceAssignmentObserver : "the Factory must accept both read-only and writable Roles"
        assert PhysicalServerResourceAssignmentObserver.isAssignableFrom(PhysicalServerResourceAssignmentController) :
                "the existing Controller must retain its Observer contract"
        [KvmResourceAssignmentController, ManagementNodeResourceAssignmentController,
         ZbsResourceAssignmentObserver].each { controller ->
            assert controller.superclass == Object : "${controller.name} must not inherit Assignment orchestration"
            assert !controller.methods.any { it.name == "handleMessage" } :
                    "${controller.name} must only implement Role operations"
            assert !controller.methods.any { it.name in ["roleServices", "getDefaultCpuCount", "getResourceConsumers"] } :
                    "${controller.name} must consume execution parameters instead of reading Profiles"
        }
        when(plugins.getExtensionList(PhysicalServerResourceAssignmentFactory.class)).thenReturn([factory, factory])
        assertFailure("Duplicate resource assignment factory") { manager.getFactory(role.toString()) }
    }

    private static class TestController implements PhysicalServerResourceAssignmentObserver {
        final String serverUuid
        final PhysicalServerRoleType roleType

        TestController(String serverUuid, PhysicalServerRoleType roleType) {
            this.serverUuid = serverUuid
            this.roleType = roleType
        }

        @Override
        boolean resourceExists() { return true }

        @Override
        void collectResourceAssignment(String uuid, List<String> serviceNames,
                ReturnValueCompletion<PhysicalServerResourceBoundary> completion) {
            completion.success(new PhysicalServerResourceBoundary(cpuSet: "1-2"))
        }
    }

    @Test
    void testControllerWriteAndUsageCapabilitiesRemainIndependent() {
        def zbs = new ZbsResourceAssignmentFactory().getResourceAssignment("server-1")
        def observationOnly = new TestController("server-1", registeredRoleType("ZBS"))
        assert zbs instanceof PhysicalServerResourceAssignmentObserver : "ZBS must expose boundary observation"
        assert !(zbs instanceof PhysicalServerResourceAssignmentController) : "ZBS must never expose Apply or Release"
        assert zbs instanceof PhysicalServerResourceUsageObserver : "ZBS must expose its service usage"
        assert !(observationOnly instanceof PhysicalServerResourceUsageObserver) :
                "boundary-only Controllers must not be forced to fabricate service usage"
    }

    private static PhysicalServerCpuTopology topology() {
        return PhysicalServerCpuTopology.from([
                "1": node("1", ["2", "3", "6", "7"], [["2", "6"], ["3", "7"]]),
                "0": node("0", ["0", "1", "4", "5"], [["0", "4"], ["1", "5"]])])
    }

    private static PhysicalServerCpuTopology largeTopology() {
        return PhysicalServerCpuTopology.from([
                "0": node("0",
                        (0..19).collect { it.toString() }, (0..9).collect { [it.toString(), (it + 10).toString()] })
        ])
    }

    private static String defaultCpuSet(
            PhysicalServerResourceAssignmentFactory factory, PhysicalServerCpuTopology topology) {
        return new PhysicalServerCpuPlanner().matchProfileCpuCount(factory.roleServices().defaultCpuCount, "",
                PhysicalServerResourceIsolationMode.SHARED, topology, Collections.emptySet())
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

    private static List<String> observedZbsServices(ZbsResourceAssignmentFactory observer) {
        return RoleServiceManifest.loadObservation(ZbsResourceAssignmentFactory.ROLE_SERVICE_MANIFEST_PATH,
                observer.roleType.toString()).managedServiceUsages("NOT_FOUND")*.serviceName
    }

    private static PhysicalServerResourceAssignmentController adapter(String roleType) {
        PhysicalServerResourceAssignmentController adapter = mock(PhysicalServerResourceAssignmentController.class)
        when(adapter.getRoleType()).thenReturn(registeredRoleType(roleType))
        return adapter
    }

    private static PhysicalServerRoleType registeredRoleType(String typeName) {
        if (KvmResourceAssignmentFactory.type.toString() == typeName) {
            return KvmResourceAssignmentFactory.type
        }
        if (ManagementNodeResourceAssignmentFactory.type.toString() == typeName) {
            return ManagementNodeResourceAssignmentFactory.type
        }
        if (ZbsResourceAssignmentFactory.type.toString() == typeName) {
            return ZbsResourceAssignmentFactory.type
        }
        PhysicalServerRoleType type = testRoleTypes[typeName]
        if (type == null) {
            type = new PhysicalServerRoleType(typeName)
            testRoleTypes[typeName] = type
        }
        return type
    }

    private static void assertFailure(String expectedMessage, Closure operation) {
        Throwable failure = shouldFail(operation)
        assert failure.message?.contains(expectedMessage) :
                "validation failure must expose the expected reason: " +
                        "expectedMessage=${expectedMessage} " +
                        "actualType=${failure.class.name} actualMessage=${failure.message}"
    }
}
