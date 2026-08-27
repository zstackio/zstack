package org.zstack.test.unittest.physicalserver

import org.junit.Test
import org.zstack.core.Platform
import org.zstack.core.componentloader.PluginRegistry
import org.zstack.core.config.GlobalConfigDef
import org.zstack.header.physicalserver.PhysicalServerCpuSet
import org.zstack.header.physicalserver.PhysicalServerCpuTopology
import org.zstack.header.physicalserver.PhysicalServerNumaNode
import org.zstack.header.physicalserver.PhysicalServerResourceApplicationMode
import org.zstack.header.physicalserver.PhysicalServerResourceControlAdapter
import org.zstack.header.physicalserver.PhysicalServerResourceIsolationMode
import org.zstack.header.physicalserver.PhysicalServerResourceUsageObserver
import org.zstack.header.physicalserver.ResourceConsumerHandle
import org.zstack.header.physicalserver.RoleServiceManifest
import org.zstack.kvm.KvmPhysicalServerAdapter
import org.zstack.kvm.KvmHostConfigChecker
import org.zstack.physicalserver.PhysicalServerCpuPlanner
import org.zstack.physicalserver.PhysicalServerResourceAssignmentGlobalConfig
import org.zstack.physicalserver.PhysicalServerResourceAssignmentReconciler
import org.zstack.physicalserver.PhysicalServerResourceControlAdapterRegistry
import org.zstack.physicalserver.PhysicalServerResourceUsageObserverRegistry
import org.zstack.portal.managementnode.ManagementNodePhysicalServerAdapter

import java.nio.file.Files
import java.nio.file.Path

import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

class PhysicalServerResourceModelCase {
    @Test
    void testResourceAssignmentDefaultPreservesUpgradeChoice() {
        def fallback = PhysicalServerResourceAssignmentGlobalConfig.class
                .getDeclaredField("ENABLED")
                .getAnnotation(GlobalConfigDef.class)
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
        def method = KvmHostConfigChecker.class.getDeclaredMethod(
                "resourceAssignmentMatches",
                String.class, Boolean.TYPE, String.class, String.class)
        method.accessible = true

        assert method.invoke(
                null, "true", true,
                "[Service]\nSlice=zstack-compute.slice",
                "/zstack.slice/zstack-compute.slice/zstack-kvmagent.service") :
                "enabled bootstrap requires both the exact drop-in and live Role membership"
        assert method.invoke(
                null, "true", true,
                "[Service]\r\nSlice=zstack-compute.slice",
                "0::/zstack.slice/zstack-compute.slice/zstack-kvmagent.service") :
                "SSH line ending conversion must not cause repeated deployment"
        assert !method.invoke(
                null, "true", true,
                "[Service]\nSlice=zstack-compute.slice",
                "/system.slice/zstack-kvmagent.service") :
                "a staged drop-in alone must not suppress the restart needed to join the Role"
        assert method.invoke(
                null, "false", true, "__ABSENT__",
                "/system.slice/zstack-kvmagent.service") :
                "disabled bootstrap requires no managed drop-in and no live Role membership"
        assert !method.invoke(
                null, "false", true, "__ABSENT__",
                "/zstack.slice/zstack-compute.slice/zstack-kvmagent.service") :
                "disabling must redeploy a KVM Agent that is still inside the Role slice"
        assert method.invoke(
                null, "true", false, "__ABSENT__",
                "/system.slice/zstack-kvmagent.service") :
                "legacy cgroup hosts keep post-start assignment without a v2 bootstrap drop-in"
        assert method.invoke(
                null, "true", false,
                "[Service]\nSlice=zstack-compute.slice",
                "/system.slice/zstack-kvmagent.service") :
                "legacy cgroup hosts let the shared Assignment Adapter own its drop-in"
        assert !method.invoke(
                null, "false", false,
                "[Service]\nSlice=zstack-compute.slice",
                "/system.slice/zstack-kvmagent.service") :
                "disabling must remove a legacy Assignment Adapter drop-in"
    }

    @Test
    void testMachineSerialNormalizationRejectsFirmwarePlaceholders() {
        assert Platform.normalizeMachineSerialNumber("  PS-SN-01  ") ==
                "ps-sn-01" :
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

        assertFailure("CPU_SET_INVALID") {
            PhysicalServerCpuSet.normalize("1-")
        }
        assertFailure("CPU_SET_INVALID") {
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
                "topology must retain every online CPU exactly once: " +
                        "expected=0-7 actual=${topology.onlineCpus}"
        assert topology.fingerprint() == "0:0,4;0:1,5;1:2,6;1:3,7" :
                "topology fingerprint must be stable across NUMA map order: " +
                        "expected=0:0,4;0:1,5;1:2,6;1:3,7 actual=${topology.fingerprint()}"

        PhysicalServerNumaNode missingCpu = node(
                "0", ["0", "1"], [["0"]])
        assertFailure("do not cover every online CPU") {
            PhysicalServerCpuTopology.from(["0": missingCpu])
        }

        PhysicalServerNumaNode duplicatedCpu = node(
                "0", ["0", "1"], [["0"], ["0", "1"]])
        assertFailure("appears in multiple core groups") {
            PhysicalServerCpuTopology.from(["0": duplicatedCpu])
        }
    }

    @Test
    void testExclusiveCpuPlanningPreservesWholeCoresAndCpuZero() {
        PhysicalServerCpuPlanner planner = new PhysicalServerCpuPlanner()
        PhysicalServerCpuTopology topology = topology()

        String shared = planner.validateAndNormalize(
                PhysicalServerResourceIsolationMode.SHARED,
                "0-1,4-5", topology, [2, 3, 6, 7])
        assert shared == "0-1,4-5" :
                "shared CPUs outside exclusive reservations must remain valid: " +
                        "expected=0-1,4-5 actual=${shared}"

        String exclusive = planner.validateAndNormalize(
                PhysicalServerResourceIsolationMode.EXCLUSIVE,
                "2-3,6-7", topology, Collections.emptySet())
        assert exclusive == "2-3,6-7" :
                "exclusive CPUs must accept complete SMT core groups: " +
                        "expected=2-3,6-7 actual=${exclusive}"

        assertFailure("CPU_SET_CONFLICT") {
            planner.validateAndNormalize(
                    PhysicalServerResourceIsolationMode.SHARED,
                    "0-2", topology, [2, 6])
        }
        assertFailure("CPU_SIBLING_SPLIT") {
            planner.validateAndNormalize(
                    PhysicalServerResourceIsolationMode.EXCLUSIVE,
                    "2", topology, Collections.emptySet())
        }
        assertFailure("CPU_ZERO_RESERVED") {
            planner.validateAndNormalize(
                    PhysicalServerResourceIsolationMode.EXCLUSIVE,
                    "0,4", topology, Collections.emptySet())
        }
    }

    @Test
    void testManagementAndComputeDefaultsUseEightAvailableCpus() {
        PhysicalServerCpuTopology large = largeTopology()
        Set<Integer> exclusive = [8, 9, 18, 19] as Set<Integer>

        String compute = new KvmPhysicalServerAdapter().getDefaultCpuSet(
                large, exclusive)
        String management = new ManagementNodePhysicalServerAdapter()
                .getDefaultCpuSet(large, exclusive)

        assert compute == "1-7,11" :
                "compute default must select eight logical CPUs without the CPU0 CoreGroup: " +
                        "expected=1-7,11 actual=${compute}"
        assert management == compute :
                "management and compute shared Roles must use the same default: " +
                        "expected=${compute} actual=${management}"
        assert Collections.disjoint(
                PhysicalServerCpuSet.parse(compute), exclusive) :
                "shared defaults must exclude CPUs reserved by Exclusive Roles: " +
                        "cpuSet=${compute} exclusive=${exclusive}"
        assert Collections.disjoint(
                PhysicalServerCpuSet.parse(compute),
                large.cpuZeroGroup.cpus) :
                "automatic shared defaults must exclude every SMT sibling of CPU0: " +
                        "cpuSet=${compute} cpuZeroGroup=${large.cpuZeroGroup.cpus}"

        String constrained = new KvmPhysicalServerAdapter().getDefaultCpuSet(
                topology(), [2, 3, 6, 7] as Set<Integer>)
        assert constrained == "1,5" :
                "a constrained default must still reserve the complete CPU0 CoreGroup: " +
                        "expected=1,5 actual=${constrained}"
    }

    @Test
    void testRoleManifestsDefineStableHandlesWithoutAdapterCodeChanges() {
        RoleServiceManifest compute = RoleServiceManifest.load(
                "physical-server-roles/compute.yaml",
                "COMPUTE",
                PhysicalServerResourceApplicationMode.RESOURCE_HANDLES)
        List<ResourceConsumerHandle> computeCore = compute.handles(
                "host-agent:core", "host-agent:aux", false)
        List<ResourceConsumerHandle> computeAll = compute.handles(
                "host-agent:core", "host-agent:aux", true)

        assert compute.sliceName == "zstack-compute.slice" :
                "compute manifest must preserve its stable Role slice: " +
                        "expected=zstack-compute.slice actual=${compute.sliceName}"
        assert compute.defaultCpuCount == 8 :
                "compute manifest must make the 8C default explicit: " +
                        "expected=8 actual=${compute.defaultCpuCount}"
        assert computeCore*.serviceName ==
                ["kvmagent", "virtlogd", "network-agent", "sharedblock-agent"] :
                "compute core handles must exclude auxiliary monitoring services: " +
                        "actual=${computeCore*.serviceName}"
        assert computeAll*.serviceName.contains("node-exporter") :
                "compute manifest must add auxiliary services by configuration: " +
                        "actual=${computeAll*.serviceName}"
        assert computeAll.find { it.serviceName == "node-exporter" }.restartable :
                "manifest restartability must reach the generated handle: " +
                        "service=node-exporter actual=${computeAll.find { it.serviceName == 'node-exporter' }.restartable}"

        List<ResourceConsumerHandle> selected = compute.handlesByServiceNames(
                ["node-exporter"], "host-agent:core", "host-agent:aux", true,
                Collections.emptyMap())
        assert selected*.serviceName == ["node-exporter"] :
                "targeted restart must resolve only explicitly selected services: " +
                        "expected=[node-exporter] actual=${selected*.serviceName}"
        assertFailure("are not defined by role") {
            compute.handlesByServiceNames(
                    ["unknown-service"], "host-agent:core", "host-agent:aux",
                    true, Collections.emptyMap())
        }

        RoleServiceManifest owner = RoleServiceManifest.load(
                "physical-server-roles/test-owner.yaml",
                "TEST_OWNER",
                PhysicalServerResourceApplicationMode.RESOURCE_HANDLES)
        ResourceConsumerHandle ownerHandle = owner.handles(
                "owner:core", "owner:aux", true,
                [ownerPidFile: "/run/test-owner.pid"])[0]
        assert ownerHandle.handleType == ResourceConsumerHandle.OWNER_PID_FILE :
                "valueFrom manifest must retain OWNER_PID_FILE identity: " +
                        "expected=OWNER_PID_FILE actual=${ownerHandle.handleType}"
        assert ownerHandle.value == "/run/test-owner.pid" :
                "valueFrom must resolve from adapter supplied values: " +
                        "expected=/run/test-owner.pid actual=${ownerHandle.value}"
        assert ownerHandle.expectedCommandToken == "/usr/bin/test-owner" :
                "PID-file handles must carry the trusted command token: " +
                        "expected=/usr/bin/test-owner actual=${ownerHandle.expectedCommandToken}"
        assertFailure("cannot be resolved") {
            owner.handles(
                    "owner:core", "owner:aux", true, Collections.emptyMap())
        }

        RoleServiceManifest zbs = RoleServiceManifest.loadObservation(
                "physical-server-roles/zbs.yaml",
                "ZBS")
        assert zbs.services*.name == [
                "zstone.share.slice",
                "zstone.cs.slice",
                "zstone.vhost.slice"
        ] :
                "ZBS observation must expose the canonical ZStone Slice names without allocation defaults: " +
                        "actual=${zbs.services*.name}"
        assert zbs.applicationMode == null &&
                zbs.sliceName == null &&
                zbs.defaultCpuCount == null :
                "an observation-only ZBS manifest must not define an apply mode or CPU plan"
        assertFailure("does not use resource handles") {
            zbs.handles("zbs", "zbs", true)
        }
        assertFailure("does not match expected roleType") {
            RoleServiceManifest.load(
                    "physical-server-roles/compute.yaml",
                    "IMAGE_STORE",
                    PhysicalServerResourceApplicationMode.RESOURCE_HANDLES)
        }
        assertFailure("provider-managed role cannot define sliceName") {
            RoleServiceManifest.load(
                    "physical-server-roles/invalid-provider-slice.yaml",
                    "INVALID_PROVIDER",
                    PhysicalServerResourceApplicationMode.PROVIDER_MANAGED)
        }
        assertFailure("restartable requires a systemd handle") {
            RoleServiceManifest.load(
                    "physical-server-roles/invalid-owner-restart.yaml",
                    "INVALID_OWNER",
                    PhysicalServerResourceApplicationMode.RESOURCE_HANDLES)
        }
    }

    @Test
    void testAdapterRegistryRejectsAmbiguousAndBrokenTopologyRoles() {
        PhysicalServerResourceControlAdapter compute = adapter("COMPUTE")
        PhysicalServerResourceControlAdapter imageStore = adapter(
                "IMAGE_STORE", "COMPUTE")
        PluginRegistry pluginRegistry = registry([compute, imageStore])

        PhysicalServerResourceControlAdapterRegistry loaded =
                PhysicalServerResourceControlAdapterRegistry.load(pluginRegistry)
        assert loaded.orderedRoleTypes() == ["COMPUTE", "IMAGE_STORE"] :
                "valid adapters must be ordered without enum registration: " +
                        "expected=[COMPUTE, IMAGE_STORE] actual=${loaded.orderedRoleTypes()}"

        loaded = PhysicalServerResourceControlAdapterRegistry.load(registry([
                compute,
                adapter("IMAGE_STORE", "COMPUTE"),
                adapter("IMAGE_STORE", "COMPUTE")
        ]))
        assert !loaded.contains("IMAGE_STORE") :
                "duplicate Role adapters must not enter the executable registry: " +
                        "roleType=IMAGE_STORE actual=${loaded.orderedRoleTypes()}"
        assert loaded.getError("IMAGE_STORE").contains(
                "RESOURCE_ASSIGNMENT_ADAPTER_AMBIGUOUS") :
                "duplicate Role adapters must expose a deterministic reason: " +
                        "actual=${loaded.getError('IMAGE_STORE')}"

        loaded = PhysicalServerResourceControlAdapterRegistry.load(registry([
                adapter("STORAGE", "MISSING")
        ]))
        assert !loaded.contains("STORAGE") :
                "adapter with a missing topology provider must be disabled: " +
                        "actual=${loaded.orderedRoleTypes()}"
        assert loaded.getError("STORAGE").contains(
                "RESOURCE_ASSIGNMENT_TOPOLOGY_ROLE_UNAVAILABLE") :
                "missing topology dependency must expose its reason: " +
                        "actual=${loaded.getError('STORAGE')}"

        PhysicalServerResourceControlAdapterRegistry ambiguous =
                PhysicalServerResourceControlAdapterRegistry.load(registry([
                        adapter("IMAGE_STORE", "COMPUTE"),
                        adapter("IMAGE_STORE", "COMPUTE"),
                        compute
                ]))
        assert ambiguous.getError("IMAGE_STORE").contains(
                "RESOURCE_ASSIGNMENT_ADAPTER_AMBIGUOUS") :
                "a broken Role must retain its complete registry validation error"
    }

    @Test
    void testUsageObserverRegistryKeepsObservationSeparateFromControl() {
        PhysicalServerResourceControlAdapter compute = adapter("COMPUTE")
        PhysicalServerResourceUsageObserver zbs = observer("ZBS")
        PluginRegistry pluginRegistry = registry(
                [compute], [compute, zbs])

        PhysicalServerResourceControlAdapterRegistry controls =
                PhysicalServerResourceControlAdapterRegistry.load(
                        pluginRegistry)
        PhysicalServerResourceUsageObserverRegistry observers =
                PhysicalServerResourceUsageObserverRegistry.load(
                        pluginRegistry)

        assert controls.orderedRoleTypes() == ["COMPUTE"] :
                "an observation-only ZBS integration must not enter the executable Assignment registry: " +
                        "actual=${controls.orderedRoleTypes()}"
        assert observers.orderedObservers()*.roleType == ["COMPUTE", "ZBS"] :
                "display must union control adapters and observation-only integrations exactly once: " +
                        "actual=${observers.orderedObservers()*.roleType}"

        PhysicalServerResourceUsageObserverRegistry ambiguous =
                PhysicalServerResourceUsageObserverRegistry.load(registry(
                        [compute], [observer("ZBS"), observer("ZBS")]))
        assert ambiguous.get("ZBS") == null :
                "multiple observers for one Role must disable that display source instead of choosing one"
    }

    @Test
    void testRefreshFailureBackoffIsBoundedAndKeepsRetrying() {
        PhysicalServerResourceAssignmentReconciler reconciler =
                new PhysicalServerResourceAssignmentReconciler()
        def nextDelay = reconciler.class.getDeclaredMethod(
                "nextRefreshRetryDelay")
        nextDelay.accessible = true

        (1..10).each { int attempt ->
            int cappedAttempt = Math.min(attempt, 8)
            long lower = 100L << (cappedAttempt - 1)
            long upper = Math.min(30000L, lower * 2)
            long actual = nextDelay.invoke(reconciler) as long
            assert actual >= lower && actual <= upper :
                    "refresh retry must use bounded exponential jitter: " +
                            "attempt=${attempt} expected=${lower}..${upper} actual=${actual}"
        }
    }

    private static PhysicalServerCpuTopology topology() {
        return PhysicalServerCpuTopology.from([
                "1": node("1", ["2", "3", "6", "7"],
                        [["2", "6"], ["3", "7"]]),
                "0": node("0", ["0", "1", "4", "5"],
                        [["0", "4"], ["1", "5"]])
        ])
    }

    private static PhysicalServerCpuTopology largeTopology() {
        return PhysicalServerCpuTopology.from([
                "0": node(
                        "0",
                        (0..19).collect { it.toString() },
                        (0..9).collect { [it.toString(), (it + 10).toString()] })
        ])
    }

    private static PhysicalServerNumaNode node(
            String nodeId, List<String> online,
            List<List<String>> coreGroups) {
        PhysicalServerNumaNode node = new PhysicalServerNumaNode()
        node.nodeId = nodeId
        node.onlineCpus = online
        node.coreGroups = coreGroups
        return node
    }

    private static PhysicalServerResourceControlAdapter adapter(
            String roleType, String topologyRoleType = null) {
        String topologyRole = topologyRoleType ?: roleType
        PhysicalServerResourceControlAdapter adapter = mock(
                PhysicalServerResourceControlAdapter.class)
        when(adapter.getRoleType()).thenReturn(roleType)
        when(adapter.getIsolationMode()).thenReturn(
                PhysicalServerResourceIsolationMode.SHARED)
        when(adapter.getApplicationMode()).thenReturn(
                PhysicalServerResourceApplicationMode.RESOURCE_HANDLES)
        when(adapter.getTopologyRoleType()).thenReturn(topologyRole)
        return adapter
    }

    private static PhysicalServerResourceUsageObserver observer(
            String roleType) {
        PhysicalServerResourceUsageObserver observer = mock(
                PhysicalServerResourceUsageObserver.class)
        when(observer.getRoleType()).thenReturn(roleType)
        return observer
    }

    private static PluginRegistry registry(
            List<PhysicalServerResourceControlAdapter> adapters,
            List<PhysicalServerResourceUsageObserver> observers = []) {
        PluginRegistry registry = mock(PluginRegistry.class)
        when(registry.getExtensionList(
                PhysicalServerResourceControlAdapter.class)).thenReturn(adapters)
        when(registry.getExtensionList(
                PhysicalServerResourceUsageObserver.class)).thenReturn(observers)
        return registry
    }

    private static void assertFailure(
            String expectedMessage, Closure operation) {
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
