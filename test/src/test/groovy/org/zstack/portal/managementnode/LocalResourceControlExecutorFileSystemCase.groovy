package org.zstack.portal.managementnode

import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.Test
import org.zstack.core.CoreGlobalProperty
import org.zstack.header.physicalserver.ManagedServiceResourceUsage
import org.zstack.header.physicalserver.PhysicalServerResourceIsolationMode
import org.zstack.header.physicalserver.ResourceConsumerHandle
import org.zstack.header.physicalserver.ResourceControlCommand
import org.zstack.utils.data.SizeUnit

import java.lang.management.ManagementFactory
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.PosixFilePermissions

import static groovy.test.GroovyAssert.shouldFail

class LocalResourceControlExecutorFileSystemCase {
    private Path temporaryRoot
    private Path v2Root
    private Path v1Root
    private Path v1MemoryRoot
    private Path v1SystemdRoot
    private Path v1CpuacctRoot
    private Path procMounts
    private Path systemdUnitRoot
    private FakeCommandExecutor commands
    private LocalResourceControlExecutor executor
    private boolean previousUnitTestOn

    @Before
    void setUp() {
        previousUnitTestOn = CoreGlobalProperty.UNIT_TEST_ON
        CoreGlobalProperty.UNIT_TEST_ON = true
        temporaryRoot = Files.createTempDirectory("resource-control-case-")
        v2Root = temporaryRoot.resolve("cgroup2")
        v1Root = temporaryRoot.resolve("cpuset")
        v1MemoryRoot = temporaryRoot.resolve("memory")
        v1SystemdRoot = temporaryRoot.resolve("systemd")
        v1CpuacctRoot = temporaryRoot.resolve("cpuacct")
        procMounts = temporaryRoot.resolve("mounts")
        systemdUnitRoot = temporaryRoot.resolve("units")
        [v2Root, v1Root, v1MemoryRoot, v1SystemdRoot,
         v1CpuacctRoot, systemdUnitRoot].each { Files.createDirectories(it) }
        put(procMounts, "")
        commands = new FakeCommandExecutor(v2Root, v1Root, v1MemoryRoot)
        executor = new LocalResourceControlExecutor(
                new LocalResourceControlExecutor.ExecutionEnvironment(
                        v2Root, v1Root, v1MemoryRoot, v1SystemdRoot,
                        [v1CpuacctRoot], procMounts, systemdUnitRoot, commands))
    }

    @After
    void cleanUp() {
        temporaryRoot?.toFile()?.deleteDir()
        CoreGlobalProperty.UNIT_TEST_ON = previousUnitTestOn
    }

    @Test
    void testV2SystemdRoleApplyInspectRestartAndRelease() {
        configureV2(true)
        Path slice = configureV2SystemdRole(true)
        long memory = SizeUnit.MEGABYTE.toByte(256)

        boolean applied = executor.apply(command("3,1-2", memory, [systemdHandle()]))

        assert applied :
                "an active service already under the requested Role boundary must be synchronized"
        assert text(slice.resolve("cpuset.cpus")) == "1-3"
        assert text(slice.resolve("memory.max")) == "${memory}"
        assert text(dropIn("zstack-management.slice")) == "[Slice]\nAllowedCPUs=1-3\nMemoryMax=${memory}"
        assert text(dropIn("prometheus.service")) == "[Service]\nSlice=zstack-management.slice"
        assert commands.count("systemctl", "daemon-reload") == 1 :
                "one transaction must reload systemd once even when two drop-ins change"

        executor.apply(command("1-3", memory, [systemdHandle()]))
        assert commands.count("systemctl", "daemon-reload") == 1 :
                "an idempotent apply must not reload systemd"

        List<ManagedServiceResourceUsage> usage = executor.inspect("MANAGEMENT", [systemdHandle()])
        assert usage.size() == 1 && usage[0].state == "RUNNING"
        assert usage[0].cpuSet == "1-3"
        assert usage[0].cpuTime == 123000L :
                "v2 usage_usec must be exported as platform nanoseconds"
        assert usage[0].memory == SizeUnit.MEGABYTE.toByte(32)
        assert usage[0].memoryLimit == memory

        executor.restart("zstack-management.slice", [systemdHandle()])
        assert commands.count("systemctl", "stop", "prometheus.service") == 1
        assert commands.count("systemctl", "start", "prometheus.service") == 1

        boolean released = executor.release(command("1-3", memory, [systemdHandle()]))
        assert released :
                "release must report Synced after restoring every Role boundary"
        assert text(slice.resolve("cpuset.cpus")) == "0-7" :
                "release must restore the parent CPU boundary while the service is still running"
        assert text(slice.resolve("memory.max")) == "max"
        assert !Files.exists(dropIn("zstack-management.slice"))
        assert text(dropIn("prometheus.service")) == "[Service]\nSlice=zstack-management.slice" :
                "release must remove limits without changing the service's Role membership"
        assert commands.count("systemctl", "daemon-reload") == 2 :
                "active slice release must reload deleted config once: expected=2 total actual=${debugState()}"
        executor.release(command("1-3", memory, [systemdHandle()]))
        assert commands.count("systemctl", "daemon-reload") == 2 :
                "unchanged active slice release must not reload: expected=2 total actual=${debugState()}"
    }

    @Test
    void testDropInDirectoryModeIgnoresUmaskAndPreservesExistingPermissions() {
        configureV2(true)
        configureV2SystemdRole(true)
        executor.apply(command("1-3", null, [systemdHandle()]))

        def directories = [dropIn("prometheus.service").parent, dropIn("zstack-management.slice").parent]
        directories.each { Path directory ->
            String actual = PosixFilePermissions.toString(Files.getPosixFilePermissions(directory))
            assert actual == "rwxr-xr-x" :
                    "new drop-in directories must be 0755 even under umask 077: path=${directory} actual=${actual}"
            Files.setPosixFilePermissions(directory, PosixFilePermissions.fromString("rwxr-x---"))
        }
        executor.apply(command("1-4", null, [systemdHandle()]))
        directories.each { Path directory ->
            String actual = PosixFilePermissions.toString(Files.getPosixFilePermissions(directory))
            assert actual == "rwxr-x---" :
                    "updating a drop-in must not chmod an existing directory: path=${directory} actual=${actual}"
        }
    }

    @Test
    void testRootOwnedDropInsRemainReadableWithoutChangingDirectoryPermissions() {
        configureV2(true)
        configureV2SystemdRole(true)
        Path serviceDropIn = dropIn("prometheus.service")
        commands.rootOwnedDropIns[serviceDropIn] = "[Service]\nSlice=zstack-management.slice\n"

        executor.restart("zstack-management.slice", [systemdHandle()])
        assert commands.count("systemctl", "start", "prometheus.service") == 1 :
                "an existing root-readable Role drop-in must authorize the requested service restart"
        assert commands.invocations.any { it.take(2) == ["sudo", "-n"] && it.last() == serviceDropIn.toString() } :
                "drop-in reads must use the same elevated permissions as writes"

        commands.rootOwnedDropIns[serviceDropIn] = "[Service]\nSlice=zstack-compute.slice\n"
        Throwable failure = shouldFail { executor.restart("zstack-management.slice", [systemdHandle()]) }
        assert failure.message.contains("is not configured for slice") :
                "a root-owned drop-in must still prevent another Role from restarting the service"
        Path retired = dropIn("retired.service")
        Files.createDirectories(retired.parent)
        commands.rootOwnedDropIns[retired] = "[Service]\nSlice=zstack-management.slice\n"
        executor.apply(command("1-3", null, [systemdHandle()]))
        assert commands.rootOwnedDropIns[serviceDropIn].contains("Slice=zstack-compute.slice") :
                "Apply must preserve the first owner's root-readable drop-in, not overwrite it as missing"
        assert !Files.exists(serviceDropIn) : "Apply must not replace another Role's inaccessible drop-in"
        assert !commands.rootOwnedDropIns.containsKey(retired) :
                "removed services must lose the managed drop-in even when its directory is root-readable only"

        Path sliceDropIn = dropIn("zstack-management.slice")
        commands.rootOwnedDropIns[sliceDropIn] = "[Slice]\nAllowedCPUs=1-3\nMemoryMax=268435456\n"
        executor.release(command("1-3", 268435456L, [systemdHandle()]))
        assert !commands.rootOwnedDropIns.containsKey(sliceDropIn) :
                "Release must remove persisted limits even when the MN user cannot read their directory"
        assert commands.rootOwnedDropIns.containsKey(serviceDropIn) :
                "Release must preserve service membership"
    }

    @Test
    void testFirstRoleToWriteSliceOwnsSharedService() {
        configureV2(true)
        configureV2SystemdRole(true)
        assert executor.apply(command("1-3", null, [systemdHandle()]))

        Path computeSlice = v2Root.resolve("zstack.slice/zstack-compute.slice")
        configureV2Group(computeSlice, "0-7", "0", "max", 0)
        commands.unit("zstack-compute.slice", true, "/zstack.slice/zstack-compute.slice")
        ResourceControlCommand compute = command("4-5", null, [systemdHandle()])
        compute.roleType = "COMPUTE"
        compute.sliceName = "zstack-compute.slice"

        assert !executor.apply(compute) : "a Role with no owned services must remain Unsynced"
        assert text(dropIn("prometheus.service")) == "[Service]\nSlice=zstack-management.slice"
        assert executor.inspect("COMPUTE", "zstack-compute.slice", [systemdHandle()]).isEmpty() :
                "service usage must be shown only under the Role that first wrote Slice="
        assert executor.release(compute)
        assert text(dropIn("prometheus.service")) == "[Service]\nSlice=zstack-management.slice" :
                "releasing another Role must preserve the first owner's service drop-in"
    }

    @Test
    void testV2ExclusiveRoleChangesPartitionWithoutRestartingServices() {
        configureV2(false)
        Path slice = configureV2SystemdRole(true, false)
        put(slice.resolve("cpuset.cpus.partition"), "member")
        put(slice.resolve("cpuset.cpus.exclusive"), "")
        ResourceControlCommand exclusive = command("2-3", null, [systemdHandle()])
        exclusive.isolationMode = PhysicalServerResourceIsolationMode.EXCLUSIVE

        boolean applied = executor.apply(exclusive)

        assert applied :
                "an active service already in its Role slice must become exclusive without restart"
        assert text(slice.resolve("cpuset.cpus")) == "2-3"
        assert text(slice.resolve("cpuset.cpus.exclusive")) == "2-3"
        assert text(slice.resolve("cpuset.cpus.partition")) == "root"
        assert commands.count("systemctl", "stop", "prometheus.service") == 0
        assert commands.count("systemctl", "start", "prometheus.service") == 0

        boolean shared = executor.apply(command("1-3", null, [systemdHandle()]))

        assert shared :
                "changing an existing Role slice back to shared must not require a service restart"
        assert text(slice.resolve("cpuset.cpus.partition")) == "member"
        assert text(slice.resolve("cpuset.cpus.exclusive")) == ""
        assert text(slice.resolve("cpuset.cpus")) == "1-3"
    }

    @Test
    void testExclusiveRoleRejectsCgroupV1BeforeWritingSystemdConfig() {
        configureV1Cpuset()
        ResourceControlCommand request = command("2-3", null, [systemdHandle()])
        request.isolationMode = PhysicalServerResourceIsolationMode.EXCLUSIVE

        Throwable failure = shouldFail { executor.apply(request) }

        assert failure?.message == "Exclusive CPU partitions require cgroup v2"
        assert !Files.exists(dropIn("zstack-management.slice")) :
                "an unsupported exclusive request must fail before changing persistent config"
    }

    @Test
    void testExclusiveRoleRequiresTheRoleSliceBoundary() {
        configureV2(false)
        commands.unit("zstack-management.slice", true, "/zstack.slice/zstack-management.slice")
        ResourceControlCommand request = command("2-3", null, [systemdHandle()])
        request.isolationMode = PhysicalServerResourceIsolationMode.EXCLUSIVE

        Throwable failure = shouldFail { executor.apply(request) }

        assert failure?.message?.contains("must be active before applying exclusive isolation") :
                "exclusive CPUs must belong to the Role slice, not separate per-service fallbacks"
    }

    @Test
    void testV2SystemdMemoryOnlyRoleDoesNotChangeCpuBoundary() {
        configureV2(true)
        Path slice = configureV2SystemdRole(true)
        long memory = SizeUnit.MEGABYTE.toByte(256)

        boolean applied = executor.apply(command(null, memory, [systemdHandle()]))

        assert applied :
                "a Role that only owns memory must not require a CPU assignment"
        assert text(slice.resolve("cpuset.cpus")) == "0-7" :
                "memory-only apply must preserve the existing CPU boundary"
        assert text(slice.resolve("memory.max")) == "${memory}"
        assert text(dropIn("zstack-management.slice")) == "[Slice]\nMemoryMax=${memory}" :
                "memory-only systemd configuration must not emit AllowedCPUs"

        boolean released = executor.release(command(null, memory, [systemdHandle()]))

        assert released :
                "memory-only release must report Synced after clearing its limit"
        assert text(slice.resolve("cpuset.cpus")) == "0-7" :
                "memory-only release must preserve the existing CPU boundary"
        assert text(slice.resolve("memory.max")) == "max"
    }

    @Test
    void testSystemdServiceIsPendingUntilItRestartsIntoTheRoleSlice() {
        configureV2(true)
        configureV2SystemdRole(false)
        long memory = SizeUnit.MEGABYTE.toByte(128)

        boolean pending = executor.apply(command("4-5", memory, [systemdHandle()]))
        assert !pending :
                "writing the drop-in must not claim that an existing process moved slices"
        List<ManagedServiceResourceUsage> pendingUsage = executor.inspect("MANAGEMENT", [systemdHandle()])
        assert pendingUsage[0].restartRequired :
                "a running service outside its configured Role slice must be exposed as restartRequired: " +
                        "expected=true actual=${pendingUsage[0].restartRequired}"

        Path service = v2Root.resolve("zstack.slice/zstack-management.slice/prometheus.service")
        configureV2Group(service, "", "0", "max", SizeUnit.MEGABYTE.toByte(16))
        put(service.resolve("cpu.stat"), "usage_usec 1\n")
        commands.unit("prometheus.service", true, "/zstack.slice/zstack-management.slice/prometheus.service")

        boolean ready = executor.apply(command("4-5", memory, [systemdHandle()]))
        assert ready :
                "the next apply must observe the service in the configured slice after restart"
        List<ManagedServiceResourceUsage> readyUsage = executor.inspect("MANAGEMENT", [systemdHandle()])
        assert !readyUsage[0].restartRequired :
                "a service already running inside its configured Role slice must not require restart: " +
                        "expected=false actual=${readyUsage[0].restartRequired}"
    }

    @Test
    void testMissingSystemdControlGroupsFailWithoutDiscardingAppliedRoleCpuBoundary() {
        configureV2(false)
        Path slice = configureV2SystemdRole(true, false)
        commands.unit("empty.service", true, "")
        commands.unit("disappeared.service", true, "/system.slice/disappeared.service")

        ["empty.service", "disappeared.service"].each { String unit ->
            Throwable failure = shouldFail(LocalResourceControlExecutor.ResourceControlException) {
                executor.apply(command("4-5", null, [systemdHandle(unit, unit), systemdHandle()]))
            }
            assert failure.message == "No control group was found for systemd unit[${unit}]" :
                    "a missing service cgroup must fail, not wait for restart: unit=${unit} actual=${failure.message}"
        }
        assert text(slice.resolve("cpuset.cpus")) == "4-5" :
                "the valid service must retain the Role CPU boundary: expected=4-5 actual=${debugState()}"
    }

    @Test
    void testRejectsMemoryLimitBelowCurrentSliceUsageBeforeChangingConfig() {
        configureV2(true)
        Path slice = configureV2SystemdRole(true)
        put(slice.resolve("memory.current"), "${SizeUnit.MEGABYTE.toByte(192)}")

        Throwable failure = shouldFail {
            executor.apply(command("0-1", SizeUnit.MEGABYTE.toByte(128), [systemdHandle()]))
        }

        assert failure?.message?.contains("is below current usage")
        assert !Files.exists(dropIn("zstack-management.slice")) :
                "an unsafe shrink must fail before persisting the desired systemd state"
    }

    @Test
    void testV2CpuAndV1MemoryWorkInHybridMode() {
        configureV2(false)
        Path cpuSlice = configureV2SystemdRole(true, false)
        configureV1MemorySystemdRole()
        long memory = SizeUnit.MEGABYTE.toByte(320)

        boolean response = executor.apply(command("2-3", memory, [systemdHandle()]))

        assert response :
                "CPU and memory controllers must be selected independently in a hybrid hierarchy"
        assert text(cpuSlice.resolve("cpuset.cpus")) == "2-3"
        assert text(v1MemoryRoot.resolve("zstack.slice/zstack-management.slice/memory.limit_in_bytes")) == "${memory}"
        assert text(dropIn("zstack-management.slice")).contains("AllowedCPUs=2-3")
        assert text(dropIn("zstack-management.slice")).contains("MemoryLimit=${memory}")

        List<ManagedServiceResourceUsage> usage = executor.inspect("MANAGEMENT", [systemdHandle()])
        assert usage[0].cpuSet == "2-3"
        assert usage[0].memory == SizeUnit.MEGABYTE.toByte(24)
        assert usage[0].memoryLimit == memory
    }

    @Test
    void testMissingActiveMemoryGroupReturnsErrorWithoutBlockingCpu() {
        configureV2(false)
        Path cpuSlice = configureV2SystemdRole(true, false)
        configureV1MemoryRoot(SizeUnit.GIGABYTE.toByte(4))

        Throwable failure = shouldFail(LocalResourceControlExecutor.ResourceControlException) {
            executor.apply(command("2-3", SizeUnit.MEGABYTE.toByte(320), [systemdHandle()]))
        }

        assert failure.message ==
                "Memory control group for active systemd slice[zstack-management.slice] does not exist" :
                "missing active memory must fail with its slice identity: actual=${failure.message}"
        assert text(cpuSlice.resolve("cpuset.cpus")) == "2-3" :
                "missing memory must preserve the independent CPU assignment: expected=2-3 actual=${debugState()}"
    }

    @Test
    void testV1CpuFallbackMovesSystemdProcessesIntoManagedCpuset() {
        configureV1Cpuset()
        String pid = currentPid()
        String sliceGroup = "/zstack.slice/zstack-management.slice"
        String serviceGroup = "${sliceGroup}/prometheus.service"
        commands.unit("zstack-management.slice", true, sliceGroup)
        commands.unit("prometheus.service", true, serviceGroup, pid)
        put(v1SystemdRoot.resolve(serviceGroup.substring(1)).resolve("cgroup.procs"), pid)

        boolean response = executor.apply(command("6-7", null, [systemdHandle()]))

        Path managed = v1Root.resolve("zstack-role-MANAGEMENT-unit-prometheus.service")
        assert response :
                "v1 shared CPU assignment must report Synced after moving the service"
        assert text(managed.resolve("cpuset.cpus")) == "6-7"
        assert text(managed.resolve("cgroup.procs")).split(/\s+/).contains(pid) :
                "v1 cpuset is not systemd delegated, so the stable unit cgroup must supply its process set"
        put(v1CpuacctRoot.resolve(v1Root.relativize(managed)).resolve("cpuacct.usage"), "987654")
        List<ManagedServiceResourceUsage> usage = executor.inspect("MANAGEMENT", [systemdHandle()])
        assert usage[0].state == "RUNNING"
        assert usage[0].cpuSet == "6-7" && usage[0].cpuTime == 987654L :
                "v1 inspection must follow the managed cpuset into cpuacct"

        boolean released = executor.release(command("6-7", null, [systemdHandle()]))
        assert released :
                "v1 release must report Synced after restoring the CPU boundary"
        assert text(managed.resolve("cpuset.cpus")) == "0-7"
    }

    @Test
    void testInactiveSliceReleaseReloadsChangedConfigOnlyOnce() {
        configureV1Cpuset()
        commands.unit("zstack-management.slice", false, "")
        commands.missingUnit("optional.service")
        put(dropIn("zstack-management.slice"), "[Slice]\nAllowedCPUs=2-3")
        ResourceControlCommand request = command("2-3", null, [systemdHandle("optional", "optional.service", true)])

        boolean released = executor.release(request)

        assert released : "inactive optional-only Role release must succeed: expected=true actual=${released}"
        assert commands.count("systemctl", "daemon-reload") == 1 :
                "removing inactive slice config must reload once: expected=1 actual=${debugState()}"
        assert !Files.exists(dropIn("zstack-management.slice")) :
                "inactive slice constraints must be removed: expected=absent actual=${debugState()}"
        executor.release(request)
        assert commands.count("systemctl", "daemon-reload") == 1 :
                "unchanged inactive slice release must not reload: expected=1 actual=${debugState()}"
    }

    @Test
    void testV1CpuFallbackReleaseClearsIndependentV1RoleMemory() {
        configureV1Cpuset()
        configureV1MemorySystemdRole()
        Path memorySlice = v1MemoryRoot.resolve("zstack.slice/zstack-management.slice")
        verifyMissingCpuSliceRelease(v1Root, memorySlice.resolve("memory.limit_in_bytes"), "4294967296", "0-7")
    }

    @Test
    void testV1CpuFallbackReleaseClearsIndependentV2RoleMemory() {
        configureV1Cpuset()
        put(v2Root.resolve("cgroup.controllers"), "memory")
        put(v2Root.resolve("memory.max"), "max")
        Path memorySlice = v2Root.resolve("zstack.slice/zstack-management.slice")
        put(memorySlice.resolve("memory.max"), "134217728")
        verifyMissingCpuSliceRelease(v1Root, memorySlice.resolve("memory.max"), "max", "0-7")
    }

    @Test
    void testV2CpuFallbackReleaseClearsIndependentV1RoleMemory() {
        configureV2(false)
        configureV1MemorySystemdRole()
        Path memorySlice = v1MemoryRoot.resolve("zstack.slice/zstack-management.slice")
        verifyMissingCpuSliceRelease(v2Root, memorySlice.resolve("memory.limit_in_bytes"), "4294967296", "")
    }

    private void verifyMissingCpuSliceRelease(Path cpuRoot, Path memoryLimit, String unlimited, String releasedCpus) {
        String sliceGroup = "/zstack.slice/zstack-management.slice"
        commands.unit("zstack-management.slice", true, sliceGroup)
        commands.unit("prometheus.service", true, "${sliceGroup}/prometheus.service")
        Path managedCpu = cpuRoot.resolve("zstack-role-MANAGEMENT-unit-prometheus.service")
        configureV2Group(managedCpu, "2-3", "0", null, 0)
        put(memoryLimit, "134217728")
        put(dropIn("zstack-management.slice"), "[Slice]\nMemoryLimit=134217728")
        put(dropIn("prometheus.service"), "[Service]\nSlice=zstack-management.slice")

        boolean released = executor.release(command("2-3", 134217728L, [systemdHandle()]))

        assert released : "independent controller release must succeed: expected=true actual=${released}"
        assert text(memoryLimit) == unlimited :
                "missing CPU slice must not skip Role memory: expected=${unlimited} actual=${text(memoryLimit)}"
        assert text(managedCpu.resolve("cpuset.cpus")) == releasedCpus :
                "legacy CPU fallback must still release: expected=${releasedCpus} actual=${debugState()}"
        assert commands.count("systemctl", "daemon-reload") == 1 :
                "fallback release must reload removed config: expected=1 actual=${debugState()}"
        assert text(dropIn("prometheus.service")) == "[Service]\nSlice=zstack-management.slice" :
                "release must preserve Role membership: expected=original Slice actual=${debugState()}"
        executor.release(command("2-3", 134217728L, [systemdHandle()]))
        assert commands.count("systemctl", "daemon-reload") == 1 :
                "unchanged fallback release must not reload again: expected=1 actual=${debugState()}"
    }

    @Test
    void testFailedCpuFallbackReleaseStillClearsRoleMemoryAndReloads() {
        configureV1Cpuset()
        configureV1MemorySystemdRole()
        commands.unit("zstack-management.slice", true, "/zstack.slice/zstack-management.slice")
        commands.missingUnit("prometheus.service")
        Path memoryLimit = v1MemoryRoot.resolve("zstack.slice/zstack-management.slice/memory.limit_in_bytes")
        put(memoryLimit, "134217728")
        put(dropIn("zstack-management.slice"), "[Slice]\nMemoryLimit=134217728")

        Throwable failure = shouldFail(LocalResourceControlExecutor.ResourceControlException) {
            executor.release(command("2-3", 134217728L, [systemdHandle()]))
        }

        assert failure.message == "Systemd unit[prometheus.service] does not exist" :
                "failed fallback release must preserve its service error: actual=${failure.message}"
        assert text(memoryLimit) == "4294967296" :
                "fallback failure must not leave Role memory constrained: " +
                        "expected=4294967296 actual=${text(memoryLimit)}"
        assert commands.count("systemctl", "daemon-reload") == 1 :
                "fallback failure must not leave deleted config cached: expected=1 actual=${debugState()}"
    }

    @Test
    void testMissingRoleMemoryInterfaceFailsReleaseAfterReload() {
        configureV1Cpuset()
        configureV1MemoryRoot(SizeUnit.GIGABYTE.toByte(4))
        Path memorySlice = v1MemoryRoot.resolve("zstack.slice/zstack-management.slice")
        Files.createDirectories(memorySlice)
        commands.unit("zstack-management.slice", true, "/zstack.slice/zstack-management.slice")
        put(dropIn("zstack-management.slice"), "[Slice]\nMemoryLimit=134217728")

        Throwable failure = shouldFail(LocalResourceControlExecutor.ResourceControlException) {
            executor.release(command("2-3", 134217728L, [systemdHandle()]))
        }

        assert failure.message == "Memory controller is unavailable for control group[${memorySlice}]" :
                "Role memory release failure must not be hidden by missing CPU slice: actual=${failure.message}"
        assert commands.count("systemctl", "daemon-reload") == 1 :
                "memory release failure must still reload deleted config: expected=1 actual=${debugState()}"
    }

    @Test
    void testConfiguredSliceNamesRoundTripThroughApplyInspectAndRestart() {
        configureV2(false)
        ["zstack-management.slice", "role-custom.slice", "role:custom.slice"].each { String sliceName ->
            Path slice = v2Root.resolve(sliceName)
            Path service = slice.resolve("prometheus.service")
            configureV2Group(slice, "0-7", "0", null, 0)
            configureV2Group(service, "", "0", null, 0)
            commands.unit(sliceName, true, "/${sliceName}")
            commands.unit("prometheus.service", true, "/${sliceName}/prometheus.service")
            Files.deleteIfExists(dropIn("prometheus.service"))
            ResourceControlCommand request = command("2-3", null, [systemdHandle()])
            request.sliceName = sliceName

            boolean applied = executor.apply(request)

            assert applied : "valid slice must round trip: slice=${sliceName} expected=true actual=${applied}"
            assert text(dropIn("prometheus.service")) == "[Service]\nSlice=${sliceName}" :
                    "apply must persist the requested slice identity: expected=${sliceName} actual=${debugState()}"
            List<ManagedServiceResourceUsage> usage = executor.inspect("MANAGEMENT", sliceName, [systemdHandle()])
            assert usage.size() == 1 && !usage[0].restartRequired :
                    "an in-slice service must not need restart: slice=${sliceName} actual=${usage}"
            assert executor.inspect("COMPUTE", "other.slice", [systemdHandle()]).isEmpty() :
                    "a foreign Role must not claim the configured slice: expected=empty actual=${debugState()}"
            executor.restart(sliceName, [systemdHandle()])
        }
        assert commands.count("systemctl", "stop", "prometheus.service") == 3 :
                "all valid slice names must pass restart ownership checks: expected=3 actual=${debugState()}"

        List<String> invalidSlices = ["../role.slice", "role/custom.slice", "-role.slice", ".role.slice"]
        invalidSlices.add("r" * 250 + ".slice")
        invalidSlices.each { String invalidSlice ->
            put(dropIn("prometheus.service"), "[Service]\nSlice=${invalidSlice}")
            Throwable failure = shouldFail(LocalResourceControlExecutor.ResourceControlException) {
                executor.restart(invalidSlice, [systemdHandle()])
            }
            assert failure.message.contains("is not configured for slice") :
                    "invalid Slice values must not establish ownership: " +
                            "slice=${invalidSlice} actual=${failure.message}"
        }
        assert commands.count("systemctl", "stop", "prometheus.service") == 3 :
                "invalid slice names must not restart services: expected=3 actual=${debugState()}"
    }

    @Test
    void testMemoryRequestFailsWhenOnlyCpuControllerExists() {
        configureV2(false)
        configureV2SystemdRole(true, false)

        Throwable failure = shouldFail {
            executor.apply(command("0-1", SizeUnit.MEGABYTE.toByte(128), [systemdHandle()]))
        }
        assert failure.message == "No available memory controller was found"

        boolean cpuOnly = executor.apply(command("0-1", null, [systemdHandle()]))
        assert cpuOnly :
                "CPU assignment must remain available independently of the memory controller"
    }

    @Test
    void testSlicedFlowDistinguishesOptionalMissingAndRequiredInactiveServices() {
        configureV2(false)
        configureV2SystemdRole(true, false)
        commands.missingUnit("optional.service")
        commands.unit("required.service", false, "/zstack.slice/zstack-management.slice/required.service")

        ResourceConsumerHandle optional = systemdHandle("optional", "optional.service", true)
        ResourceConsumerHandle required = systemdHandle("required", "required.service", false)
        Throwable failure = shouldFail(LocalResourceControlExecutor.ResourceControlException) {
            executor.apply(command("0-1", null, [optional, required]))
        }
        assert failure.message == "Systemd unit[required.service] is not active" :
                "required inactivity must fail rather than report pending restart: actual=${failure.message}"

        commands.missingUnit("required.service")
        failure = shouldFail(LocalResourceControlExecutor.ResourceControlException) {
            executor.apply(command("0-1", null, [optional, required]))
        }
        assert failure.message == "Systemd unit[required.service] does not exist" :
                "required absence must retain its service identity: actual=${failure.message}"

        boolean response = executor.apply(command("0-1", null, [optional, systemdHandle()]))
        assert response : "optional absence must not fail a ready Role: expected=true actual=${response}"
    }

    @Test
    void testRestartReportsMissingInactiveAndFailedUnits() {
        configureV2(false)
        configureV2SystemdRole(true, false)
        ResourceConsumerHandle handle = systemdHandle()
        commands.missingUnit(handle.value)
        assert shouldFail { executor.restart("zstack-management.slice", [handle]) }?.message ==
                "Systemd unit[prometheus.service] does not exist"

        commands.unit(handle.value, false, "/system.slice/prometheus.service")
        assert shouldFail { executor.restart("zstack-management.slice", [handle]) }?.message ==
                "Systemd unit[prometheus.service] is not active"

        commands.unit(handle.value, true, "/system.slice/prometheus.service")
        assert shouldFail { executor.restart("zstack-management.slice", [handle]) }?.message ==
                "Systemd unit[prometheus.service] is not configured for slice[zstack-management.slice]"

        put(dropIn(handle.value), "[Service]\nSlice=zstack-management.slice")
        commands.unit("zstack-management.slice", true, "")
        assert shouldFail { executor.restart("zstack-management.slice", [handle]) }?.message ==
                "Systemd slice[zstack-management.slice] is not active in the cpuset hierarchy"

        commands.unit("zstack-management.slice", true, "/zstack.slice/zstack-management.slice")
        commands.failStarts.add(handle.value)
        assert shouldFail { executor.restart("zstack-management.slice", [handle]) }?.message ==
                "Systemd unit[prometheus.service] is not active after restart"
    }

    @Test
    void testRestartFailureDoesNotStopTheRemainingServices() {
        configureV1Cpuset()
        commands.unit("zstack-management.slice", true, "/zstack.slice/zstack-management.slice")
        List<ResourceConsumerHandle> handles = ["first", "second", "third"].collect { name ->
            String unit = "${name}.service"
            commands.unit(unit, true, "/system.slice/${unit}")
            put(dropIn(unit), "[Service]\nSlice=zstack-management.slice")
            return systemdHandle(name, unit)
        }
        commands.failStarts.add("second.service")

        Throwable failure = shouldFail { executor.restart("zstack-management.slice", handles) }

        assert failure.message == "Systemd unit[second.service] is not active after restart" :
                "the first failed service must be reported: actual=${failure.message}"
        List<List<String>> operations = commands.invocations.collect { it.drop(2) }.findAll {
            it.size() > 1 && it[0] == "systemctl" && it[1] in ["stop", "start"]
        }
        assert operations == [["systemctl", "stop", "first.service"], ["systemctl", "start", "first.service"],
                              ["systemctl", "stop", "second.service"], ["systemctl", "start", "second.service"]] :
                "restart must complete each service before touching the next; third must stay running: ${operations}"
        assert commands.units["first.service"].ActiveState == "active" : "first service must already be restored"
        assert commands.units["third.service"].ActiveState == "active" : "unprocessed service must remain running"
    }

    @Test
    void testRestartAllowsV1SystemdSliceOutsideCpusetHierarchy() {
        configureV1Cpuset()
        ResourceConsumerHandle handle = systemdHandle()
        commands.unit("zstack-management.slice", true, "/zstack.slice/zstack-management.slice")
        commands.unit(handle.value, true, "/system.slice/prometheus.service")
        put(dropIn(handle.value), "[Service]\nSlice=zstack-management.slice")

        executor.restart("zstack-management.slice", [handle])

        assert commands.count("systemctl", "stop", "prometheus.service") == 1
        assert commands.count("systemctl", "start", "prometheus.service") == 1
    }

    @Test
    void testRestartFailsWhenServiceDoesNotEnterConfiguredSlice() {
        configureV2(false)
        configureV2SystemdRole(false, false)
        ResourceConsumerHandle handle = systemdHandle()
        put(dropIn(handle.value), "[Service]\nSlice=zstack-management.slice")

        assert shouldFail { executor.restart("zstack-management.slice", [handle]) }?.message ==
                "Systemd unit[prometheus.service] did not enter slice[zstack-management.slice] after restart"
    }

    @Test
    void testOptionalMissingServiceIsSkippedButRequiredServiceFails() {
        configureV2(false)
        commands.missingUnit("optional.service")
        commands.missingUnit("required.service")
        ResourceConsumerHandle optional = systemdHandle("optional", "optional.service", true)
        ResourceConsumerHandle required = systemdHandle("required", "required.service", false)
        ResourceControlCommand request = command("0-1", null, [optional, required])
        request.sliceName = null

        Throwable failure = shouldFail { executor.apply(request) }
        assert failure.message == "Systemd unit[required.service] does not exist" :
                "optional absence must not hide a required-service failure"
    }

    @Test
    void testUnavailableControllersFailRoleInspection() {
        ResourceControlCommand request = command("0-1", null, [systemdHandle()])
        request.sliceName = null

        Throwable applyFailure = shouldFail { executor.apply(request) }
        Throwable inspectFailure = shouldFail { executor.inspect("MANAGEMENT", [systemdHandle()]) }

        assert applyFailure.message == "No available cpuset controller was found" :
                "an unavailable cpuset backend must fail the Assignment"
        assert inspectFailure.message == "No available cpuset controller was found" :
                "an unavailable cgroup backend must fail the Role observation instead of fabricating service state"
    }

    @Test
    void testInspectionFallsBackToMainPidWhenSystemdOmitsControlGroup() {
        configureV2(false)
        String pid = currentPid()
        String processGroupLine = new File("/proc/${pid}/cgroup").readLines().find { it.startsWith("0::") }
        Assume.assumeTrue("the MainPID fallback case requires a cgroup v2 process entry", processGroupLine != null)
        String processGroup = processGroupLine.substring(3)
        Path target = v2Root.resolve(processGroup.startsWith("/") ? processGroup.substring(1) : processGroup)
        configureV2Group(target, "5", "0", null, 0)
        commands.unit("prometheus.service", true, "", pid)

        List<ManagedServiceResourceUsage> usage = executor.inspect("MANAGEMENT", [systemdHandle()])

        assert usage[0].state == "RUNNING" && usage[0].cpuSet == "5" :
                "MainPID must keep usage observable when systemd returns no ControlGroup"
    }

    private void configureV2(boolean memory) {
        Files.createDirectories(v2Root)
        put(v2Root.resolve("cgroup.controllers"), memory ? "cpuset memory cpu" : "cpuset cpu")
        put(v2Root.resolve("cgroup.subtree_control"), "")
        put(v2Root.resolve("cpuset.cpus.effective"), "0-7")
        put(v2Root.resolve("cpuset.mems.effective"), "0")
        put(v2Root.resolve("cgroup.procs"), "")
        if (memory) {
            put(v2Root.resolve("memory.max"), "max")
            put(v2Root.resolve("memory.current"), "0")
        }
    }

    private Path configureV2SystemdRole(boolean serviceInSlice, boolean memory = true) {
        Path parent = v2Root.resolve("zstack.slice")
        configureV2Group(parent, "0-7", "0", memory ? "max" : null, 0)
        Path slice = v2Root.resolve("zstack.slice/zstack-management.slice")
        configureV2Group(slice, "0-7", "0", memory ? "max" : null, SizeUnit.MEGABYTE.toByte(64))
        commands.unit("zstack-management.slice", true, "/zstack.slice/zstack-management.slice")
        Path service = serviceInSlice
                ? slice.resolve("prometheus.service") : v2Root.resolve("system.slice/prometheus.service")
        configureV2Group(service, "", "0", memory ? "max" : null, SizeUnit.MEGABYTE.toByte(32))
        put(service.resolve("cpu.stat"), "usage_usec 123\n")
        commands.unit("prometheus.service", true, "/${v2Root.relativize(service)}")
        return slice
    }

    private void configureV2Group(Path group, String cpus, String mems, String memoryLimit, long memoryCurrent) {
        Files.createDirectories(group)
        put(group.resolve("cpuset.cpus"), cpus)
        put(group.resolve("cpuset.mems"), mems)
        put(group.resolve("cgroup.procs"), "")
        if (memoryLimit != null) {
            put(group.resolve("memory.max"), memoryLimit)
            put(group.resolve("memory.current"), "${memoryCurrent}")
        }
    }

    private void configureV1Cpuset() {
        put(v1Root.resolve("cpuset.cpus"), "0-7")
        put(v1Root.resolve("cpuset.mems"), "0")
        put(v1Root.resolve("cgroup.procs"), "")
    }

    private void configureV1MemorySystemdRole() {
        long rootLimit = SizeUnit.GIGABYTE.toByte(4)
        configureV1MemoryRoot(rootLimit)
        Path slice = v1MemoryRoot.resolve("zstack.slice/zstack-management.slice")
        configureV1MemoryGroup(slice, rootLimit, SizeUnit.MEGABYTE.toByte(64))
        configureV1MemoryGroup(slice.resolve("prometheus.service"), rootLimit, SizeUnit.MEGABYTE.toByte(24))
    }

    private void configureV1MemoryRoot(long rootLimit) {
        put(v1MemoryRoot.resolve("memory.limit_in_bytes"), "${rootLimit}")
        put(v1MemoryRoot.resolve("memory.usage_in_bytes"), "0")
        put(v1MemoryRoot.resolve("cgroup.procs"), "")
    }

    private void configureV1MemoryGroup(Path group, long limit, long usage) {
        Files.createDirectories(group)
        put(group.resolve("memory.limit_in_bytes"), "${limit}")
        put(group.resolve("memory.usage_in_bytes"), "${usage}")
        put(group.resolve("cgroup.procs"), "")
    }

    private Path dropIn(String unit) {
        return systemdUnitRoot.resolve(unit + ".d").resolve("50-zstack-resource-assignment.conf")
    }

    private String debugState() {
        List<String> files = []
        temporaryRoot.toFile().eachFileRecurse { File file ->
            if (file.isFile()) {
                files.add("${temporaryRoot.relativize(file.toPath())}=${text(file.toPath())}")
            }
        }
        return "commands=${commands.invocations}, files=${files.sort()}"
    }

    private static ResourceControlCommand command(String cpuSet, Long memory, List<ResourceConsumerHandle> handles) {
        ResourceControlCommand command = new ResourceControlCommand()
        command.roleType = "MANAGEMENT"
        command.cpuSet = cpuSet
        command.memory = memory
        command.sliceName = "zstack-management.slice"
        command.handles = handles
        return command
    }

    private static ResourceConsumerHandle systemdHandle(
            String serviceName = "prometheus", String unit = "prometheus.service", boolean optional = false) {
        ResourceConsumerHandle handle = new ResourceConsumerHandle()
        handle.handleType = ResourceConsumerHandle.SYSTEMD_UNIT
        handle.value = unit
        handle.serviceName = serviceName
        handle.optional = optional
        handle.restartable = true
        return handle
    }

    private static String currentPid() {
        return ManagementFactory.runtimeMXBean.name.split("@")[0]
    }

    private static void put(Path path, String value) {
        Files.createDirectories(path.parent)
        Files.write(path, value.getBytes(StandardCharsets.US_ASCII))
    }

    private static String text(Path path) {
        return new String(Files.readAllBytes(path), StandardCharsets.US_ASCII).trim()
    }

    private static class FakeCommandExecutor implements LocalResourceControlExecutor.CommandExecutor {
        private final Path v2Root
        private final Path v1Root
        private final Path v1MemoryRoot
        private final Map<String, Map<String, String>> units = [:]
        private final List<List<String>> invocations = []
        private final Set<String> failStarts = [] as Set
        private final Map<Path, String> rootOwnedDropIns = [:]

        FakeCommandExecutor(Path v2Root, Path v1Root, Path v1MemoryRoot) {
            this.v2Root = v2Root
            this.v1Root = v1Root
            this.v1MemoryRoot = v1MemoryRoot
        }

        void unit(String name, boolean active, String controlGroup, String mainPid = "0") {
            units[name] = [
                    LoadState: "loaded",
                    ActiveState: active ? "active" : "inactive", ControlGroup: controlGroup, MainPID: mainPid]
        }

        void missingUnit(String name) {
            units[name] = [LoadState: "not-found", ActiveState: "inactive", ControlGroup: "", MainPID: "0"]
        }

        int count(String... suffix) {
            List<String> expected = suffix.toList()
            return invocations.count { List<String> command ->
                List<String> normalized = normalize(command)
                normalized == expected
            }
        }

        @Override
        String run(byte[] input, String... command) {
            List<String> raw = command.toList()
            invocations.add(new ArrayList<>(raw))
            List<String> args = normalize(raw)
            if (args[0] == "systemctl") {
                return systemctl(args)
            }
            if (args[0] == "sh" && args[1] == "-c" && args[-2] == "resource-control-read") {
                Path path = java.nio.file.Paths.get(args[-1])
                return rootOwnedDropIns.containsKey(path) ? rootOwnedDropIns[path] :
                        (Files.isRegularFile(path) ? Files.readAllLines(path).join("\n") + "\n" : "")
            }
            if (args[0] == "mkdir" && args[1] == "-p") {
                Path path = java.nio.file.Paths.get(args[-1])
                Process process = new ProcessBuilder(["sh", "-c", 'umask 077; exec "$@"', "case-mkdir"] + args).start()
                assert process.waitFor() == 0 : "fixture mkdir failed: ${process.errorStream.text}"
                initializeKernelFiles(path)
                return ""
            }
            if (args[0] == "chmod") {
                return ""
            }
            if (args[0] == "install") {
                Path source = java.nio.file.Paths.get(args[-2])
                Path destination = java.nio.file.Paths.get(args[-1])
                Files.createDirectories(destination.parent)
                Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING)
                return ""
            }
            if (args[0] == "rm" && args[1] == "-f") {
                rootOwnedDropIns.remove(java.nio.file.Paths.get(args[2]))
                Files.deleteIfExists(java.nio.file.Paths.get(args[2]))
                return ""
            }
            if (args[0] == "tee") {
                writeKernelFile(java.nio.file.Paths.get(args[1]), input)
                return input == null ? "" :
                        new String(input, StandardCharsets.US_ASCII)
            }
            if (args == ["getconf", "CLK_TCK"]) {
                return "100\n"
            }
            throw new AssertionError("unexpected command: ${raw}")
        }

        private String systemctl(List<String> args) {
            String action = args[1]
            if (action == "show") {
                Map<String, String> properties = units[args[2]] ?: [
                        LoadState: "not-found", ActiveState: "inactive", ControlGroup: "", MainPID: "0"]
                return properties.collect { key, value ->
                    "${key}=${value}"
                }.join("\n") + "\n"
            }
            if (action == "daemon-reload") {
                return ""
            }
            if (action == "start" || action == "stop") {
                args.drop(2).each { String unit ->
                    if (!units.containsKey(unit)) {
                        units[unit] = [LoadState: "loaded", ActiveState: "inactive", ControlGroup: "", MainPID: "0"]
                    }
                    units[unit].ActiveState = action == "start" && !failStarts.contains(unit) ? "active" : "inactive"
                }
                return ""
            }
            throw new AssertionError("unexpected systemctl command: ${args}")
        }

        private void initializeKernelFiles(Path path) {
            if (path.startsWith(v2Root) && path != v2Root) {
                putIfMissing(path.resolve("cpuset.cpus"), "")
                putIfMissing(path.resolve("cpuset.mems"), "")
                putIfMissing(path.resolve("cgroup.procs"), "")
            }
            if (path.startsWith(v1Root) && path != v1Root) {
                putIfMissing(path.resolve("cpuset.cpus"), "")
                putIfMissing(path.resolve("cpuset.mems"), "")
                putIfMissing(path.resolve("cgroup.procs"), "")
            }
            if (path.startsWith(v1MemoryRoot) && path != v1MemoryRoot) {
                putIfMissing(path.resolve("memory.limit_in_bytes"),
                        LocalResourceControlExecutorFileSystemCase.text(v1MemoryRoot.resolve("memory.limit_in_bytes")))
                putIfMissing(path.resolve("memory.usage_in_bytes"), "0")
                putIfMissing(path.resolve("cgroup.procs"), "")
            }
        }

        private void writeKernelFile(Path path, byte[] input) {
            String value = input == null ? "" :
                    new String(input, StandardCharsets.US_ASCII)
            if (path.fileName.toString() == "cgroup.subtree_control" && value.contains("+memory")) {
                path.parent.toFile().eachDir { File child ->
                    putIfMissing(child.toPath().resolve("memory.max"), "max")
                    putIfMissing(child.toPath().resolve("memory.current"), "0")
                }
            }
            if (path.fileName.toString() == "cgroup.procs") {
                Set<String> pids = [] as LinkedHashSet
                if (Files.isRegularFile(path)) {
                    pids.addAll(LocalResourceControlExecutorFileSystemCase.text(path).split(/\s+/).findAll { it })
                }
                pids.addAll(value.trim().split(/\s+/).findAll { it })
                LocalResourceControlExecutorFileSystemCase.put(path, pids.join("\n"))
                return
            }
            LocalResourceControlExecutorFileSystemCase.put(path, value)
        }

        private static void putIfMissing(Path path, String value) {
            if (!Files.exists(path)) {
                LocalResourceControlExecutorFileSystemCase.put(path, value)
            }
        }

        private static List<String> normalize(List<String> command) {
            return command.size() >= 2 && command[0] == "sudo" && command[1] == "-n" ? command.drop(2) : command
        }
    }
}
