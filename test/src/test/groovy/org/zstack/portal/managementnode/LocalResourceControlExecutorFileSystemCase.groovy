package org.zstack.portal.managementnode

import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.Test
import org.zstack.core.CoreGlobalProperty
import org.zstack.header.physicalserver.ManagedServiceResourceUsage
import org.zstack.header.physicalserver.ResourceConsumerHandle
import org.zstack.header.physicalserver.ResourceControlCommand
import org.zstack.header.physicalserver.ResourceControlResponse
import org.zstack.utils.data.SizeUnit

import java.lang.management.ManagementFactory
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.PosixFilePermission

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
                        [v1CpuacctRoot], procMounts, systemdUnitRoot,
                        commands))
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

        ResourceControlResponse applied = executor.apply(command(
                "APPLY", "3,1-2", memory, [systemdHandle()]))

        assert applied.results*.state == ["READY"] :
                "an active service already under the Role slice must be ready: ${applied.results*.state}"
        assert applied.cpuSet == "1-3" && applied.memory == memory :
                "the live v2 slice must expose the applied CPU and memory boundary: ${applied.cpuSet}/${applied.memory}"
        assert text(slice.resolve("cpuset.cpus")) == "1-3"
        assert text(slice.resolve("memory.max")) == "${memory}"
        assert text(dropIn("zstack-management.slice")) ==
                "[Slice]\nAllowedCPUs=1-3\nMemoryMax=${memory}"
        assert text(dropIn("prometheus.service")) ==
                "[Service]\nSlice=zstack-management.slice"
        assert commands.count("systemctl", "daemon-reload") == 1 :
                "one transaction must reload systemd once even when two drop-ins change"

        executor.apply(command(
                "APPLY", "1-3", memory, [systemdHandle()]))
        assert commands.count("systemctl", "daemon-reload") == 1 :
                "an idempotent reconcile must not reload systemd"

        List<ManagedServiceResourceUsage> usage = executor.inspect(
                "MANAGEMENT", [systemdHandle()])
        assert usage.size() == 1 && usage[0].state == "RUNNING"
        assert usage[0].cpuSet == "1-3"
        assert usage[0].cpuTime == 123000L :
                "v2 usage_usec must be exported as platform nanoseconds"
        assert usage[0].memory == SizeUnit.MEGABYTE.toByte(32)
        assert usage[0].memoryLimit == memory

        executor.restart([systemdHandle()])
        assert commands.count("systemctl", "stop", "prometheus.service") == 1
        assert commands.count("systemctl", "start", "prometheus.service") == 1

        ResourceControlResponse released = executor.apply(command(
                "RELEASE", "1-3", memory, [systemdHandle()]))
        assert released.results*.state == ["DISABLED"]
        assert released.cpuSet == "" && released.memory == 0L
        assert text(slice.resolve("cpuset.cpus")) == "0-7" :
                "release must restore the parent CPU boundary while the service is still running"
        assert text(slice.resolve("memory.max")) == "max"
        assert !Files.exists(dropIn("zstack-management.slice"))
        assert !Files.exists(dropIn("prometheus.service"))
    }

    @Test
    void testSystemdServiceIsPendingUntilItRestartsIntoTheRoleSlice() {
        configureV2(true)
        configureV2SystemdRole(false)
        long memory = SizeUnit.MEGABYTE.toByte(128)

        ResourceControlResponse pending = executor.apply(command(
                "APPLY", "4-5", memory, [systemdHandle()]))
        assert pending.results*.state == ["PENDING_RESTART"] :
                "writing the drop-in must not claim that an existing process moved slices"
        List<ManagedServiceResourceUsage> pendingUsage = executor.inspect(
                "MANAGEMENT", [systemdHandle()])
        assert pendingUsage[0].restartRequired :
                "a running service outside its configured Role slice must be exposed as restartRequired: " +
                        "expected=true actual=${pendingUsage[0].restartRequired}"

        Path service = v2Root.resolve(
                "zstack.slice/zstack-management.slice/prometheus.service")
        configureV2Group(service, "", "0", "max", SizeUnit.MEGABYTE.toByte(16))
        put(service.resolve("cpu.stat"), "usage_usec 1\n")
        commands.unit("prometheus.service", true,
                "/zstack.slice/zstack-management.slice/prometheus.service")

        ResourceControlResponse ready = executor.apply(command(
                "APPLY", "4-5", memory, [systemdHandle()]))
        assert ready.results*.state == ["READY"] :
                "the next reconcile must observe the service in the configured slice after restart"
        List<ManagedServiceResourceUsage> readyUsage = executor.inspect(
                "MANAGEMENT", [systemdHandle()])
        assert !readyUsage[0].restartRequired :
                "a service already running inside its configured Role slice must not require restart: " +
                        "expected=false actual=${readyUsage[0].restartRequired}"
    }

    @Test
    void testMissingSystemdControlGroupsDoNotBlockOtherServices() {
        configureV2(false)
        configureV2SystemdRole(true, false)
        commands.unit("empty.service", true, "")
        commands.unit("disappeared.service", true,
                "/system.slice/disappeared.service")

        ResourceControlResponse response = executor.apply(command(
                "APPLY", "4-5", null, [
                        systemdHandle("empty", "empty.service"),
                        systemdHandle("disappeared", "disappeared.service"),
                        systemdHandle()
                ]))

        assert response.results*.state == [
                "PENDING_RESTART", "PENDING_RESTART", "READY"
        ] :
                "one stale systemd ControlGroup must not discard another service's successful apply"
        assert response.coveredServiceCount == 1 &&
                response.expectedServiceCount == 3
    }

    @Test
    void testRejectsMemoryLimitBelowCurrentSliceUsageBeforeChangingConfig() {
        configureV2(true)
        Path slice = configureV2SystemdRole(true)
        put(slice.resolve("memory.current"),
                "${SizeUnit.MEGABYTE.toByte(192)}")

        Throwable failure = capture {
            executor.apply(command(
                    "APPLY", "0-1", SizeUnit.MEGABYTE.toByte(128),
                    [systemdHandle()]))
        }

        assert failure?.message == "MEMORY_LIMIT_BELOW_CURRENT_USAGE"
        assert !Files.exists(dropIn("zstack-management.slice")) :
                "an unsafe shrink must fail before persisting the desired systemd state"
    }

    @Test
    void testV2CpuAndV1MemoryWorkInHybridMode() {
        configureV2(false)
        Path cpuSlice = configureV2SystemdRole(true, false)
        configureV1MemorySystemdRole()
        long memory = SizeUnit.MEGABYTE.toByte(320)

        ResourceControlResponse response = executor.apply(command(
                "APPLY", "2-3", memory, [systemdHandle()]))

        assert response.results*.state == ["READY"] :
                "CPU and memory controllers must be selected independently in a hybrid hierarchy"
        assert text(cpuSlice.resolve("cpuset.cpus")) == "2-3"
        assert text(v1MemoryRoot.resolve(
                "zstack.slice/zstack-management.slice/memory.limit_in_bytes")) ==
                "${memory}"
        assert text(dropIn("zstack-management.slice")).contains(
                "AllowedCPUs=2-3")
        assert text(dropIn("zstack-management.slice")).contains(
                "MemoryLimit=${memory}")

        List<ManagedServiceResourceUsage> usage = executor.inspect(
                "MANAGEMENT", [systemdHandle()])
        assert usage[0].cpuSet == "2-3"
        assert usage[0].memory == SizeUnit.MEGABYTE.toByte(24)
        assert usage[0].memoryLimit == memory
    }

    @Test
    void testMissingActiveMemoryGroupReturnsErrorWithoutBlockingCpu() {
        configureV2(false)
        Path cpuSlice = configureV2SystemdRole(true, false)
        configureV1MemoryRoot(SizeUnit.GIGABYTE.toByte(4))

        ResourceControlResponse response = executor.apply(command(
                "APPLY", "2-3", SizeUnit.MEGABYTE.toByte(320),
                [systemdHandle()]))

        assert response.results*.state == ["ERROR"] :
                "a missing active memory cgroup must remain an explicit memory failure"
        assert text(cpuSlice.resolve("cpuset.cpus")) == "2-3" :
                "an unavailable memory cgroup must not block the independent CPU assignment"
    }

    @Test
    void testV1CpuFallbackMovesSystemdProcessesIntoManagedCpuset() {
        configureV1Cpuset()
        String pid = currentPid()
        String sliceGroup = "/zstack.slice/zstack-management.slice"
        String serviceGroup = "${sliceGroup}/prometheus.service"
        commands.unit("zstack-management.slice", true, sliceGroup)
        commands.unit("prometheus.service", true, serviceGroup, pid)
        put(v1SystemdRoot.resolve(serviceGroup.substring(1))
                .resolve("cgroup.procs"), pid)

        ResourceControlResponse response = executor.apply(command(
                "APPLY", "6-7", null, [systemdHandle()]))

        Path managed = v1Root.resolve(
                "zstack-role-MANAGEMENT-unit-prometheus.service")
        assert response.results*.state == ["READY"]
        assert text(managed.resolve("cpuset.cpus")) == "6-7"
        assert text(managed.resolve("cgroup.procs")).split(/\s+/).contains(pid) :
                "v1 cpuset is not systemd delegated, so the stable unit cgroup must supply its process set"
        put(v1CpuacctRoot.resolve(v1Root.relativize(managed))
                .resolve("cpuacct.usage"), "987654")
        List<ManagedServiceResourceUsage> usage = executor.inspect(
                "MANAGEMENT", [systemdHandle()])
        assert usage[0].state == "RUNNING"
        assert usage[0].cpuSet == "6-7" && usage[0].cpuTime == 987654L :
                "v1 inspection must follow the managed cpuset into cpuacct"

        ResourceControlResponse released = executor.apply(command(
                "RELEASE", "6-7", null, [systemdHandle()]))
        assert released.results*.state == ["DISABLED"]
        assert text(managed.resolve("cpuset.cpus")) == "0-7"
    }

    @Test
    void testOwnerPidFileHandleAppliesAndReleasesAUnifiedV2Group() {
        configureV2(true)
        put(v2Root.resolve("cgroup.procs"), "")
        String pid = currentPid()
        ResourceConsumerHandle owner = ownerHandle("owner")
        long memory = memoryLimitAboveCurrentProcessUsage()
        ResourceControlCommand apply = command(
                "APPLY", "0", memory, [owner])
        apply.sliceName = null

        ResourceControlResponse response = executor.apply(apply)

        Path managed = v2Root.resolve("zstack-role-MANAGEMENT-owner-owner")
        assert response.results*.state == ["READY"] :
                "owner apply failed: ${debugState()}"
        assert text(managed.resolve("cpuset.cpus")) == "0"
        assert text(managed.resolve("memory.max")) == "${memory}"
        assert text(managed.resolve("cgroup.procs")).split(/\s+/).contains(pid)

        ResourceControlCommand release = command(
                "RELEASE", "0", memory, [owner])
        release.sliceName = null
        ResourceControlResponse released = executor.apply(release)
        assert released.results*.state == ["DISABLED"]
        assert text(managed.resolve("cpuset.cpus")) == ""
        assert text(managed.resolve("memory.max")) == "max"
        assert text(v2Root.resolve("cgroup.procs")).split(/\s+/).contains(pid) :
                "release must move a managed v2 process back to its parent before clearing cpuset"
    }

    @Test
    void testOwnerPidFileWorksWithV1CpuAndMemoryControllers() {
        configureV1Cpuset()
        String pid = currentPid()
        ResourceConsumerHandle owner = ownerHandle("legacy-owner")
        long memory = memoryLimitAboveCurrentProcessUsage()
        long rootMemory = memory + SizeUnit.GIGABYTE.toByte(4)
        configureV1MemoryRoot(rootMemory)
        ResourceControlCommand apply = command(
                "APPLY", "1", memory, [owner])
        apply.sliceName = null

        ResourceControlResponse response = executor.apply(apply)

        Path cpu = v1Root.resolve(
                "zstack-role-MANAGEMENT-owner-legacy-owner")
        Path memoryGroup = v1MemoryRoot.resolve(
                "zstack-role-MANAGEMENT-owner-legacy-owner")
        assert response.results*.state == ["READY"] : debugState()
        assert text(cpu.resolve("cpuset.cpus")) == "1"
        assert text(memoryGroup.resolve("memory.limit_in_bytes")) == "${memory}"
        assert text(memoryGroup.resolve("cgroup.procs"))
                .split(/\s+/).contains(pid)

        ResourceControlCommand release = command(
                "RELEASE", "1", memory, [owner])
        release.sliceName = null
        ResourceControlResponse released = executor.apply(release)
        assert released.results*.state == ["DISABLED"]
        assert text(cpu.resolve("cpuset.cpus")) == "0-7"
        assert text(memoryGroup.resolve("memory.limit_in_bytes")) ==
                "${rootMemory}"
        assert text(v1MemoryRoot.resolve("cgroup.procs"))
                .split(/\s+/).contains(pid)
    }

    @Test
    void testMemoryRequestFailsWhenOnlyCpuControllerExists() {
        configureV2(false)
        configureV2SystemdRole(true, false)

        ResourceControlResponse withMemory = executor.apply(command(
                "APPLY", "0-1", SizeUnit.MEGABYTE.toByte(128),
                [systemdHandle()]))
        assert withMemory.results*.state == ["ERROR"] :
                "missing memory controller must not be reported as a synchronized memory limit"

        ResourceControlResponse cpuOnly = executor.apply(command(
                "APPLY", "0-1", null, [systemdHandle()]))
        assert cpuOnly.results*.state == ["READY"] :
                "CPU assignment must remain available independently of the memory controller"
    }

    @Test
    void testSlicedFlowDistinguishesOptionalMissingAndRequiredInactiveServices() {
        configureV2(false)
        configureV2SystemdRole(true, false)
        commands.missingUnit("optional.service")
        commands.unit("required.service", false,
                "/zstack.slice/zstack-management.slice/required.service")

        ResourceControlResponse response = executor.apply(command(
                "APPLY", "0-1", null, [
                        systemdHandle("optional", "optional.service", true),
                        systemdHandle("required", "required.service", false)
                ]))

        assert response.results*.state == ["SKIPPED", "ERROR"]
        assert response.expectedServiceCount == 1 &&
                response.coveredServiceCount == 0
    }

    @Test
    void testRestartReportsMissingInactiveAndFailedUnits() {
        ResourceConsumerHandle handle = systemdHandle()
        commands.missingUnit(handle.value)
        assert capture { executor.restart([handle]) }?.message ==
                "SYSTEMD_UNIT_NOT_FOUND"

        commands.unit(handle.value, false, "/system.slice/prometheus.service")
        assert capture { executor.restart([handle]) }?.message ==
                "SYSTEMD_UNIT_NOT_ACTIVE"

        commands.unit(handle.value, true, "/system.slice/prometheus.service")
        commands.failStarts.add(handle.value)
        assert capture { executor.restart([handle]) }?.message ==
                "SYSTEMD_UNIT_RESTART_FAILED"
    }

    @Test
    void testOptionalMissingServiceIsSkippedButRequiredServiceFails() {
        configureV2(false)
        commands.missingUnit("optional.service")
        commands.missingUnit("required.service")
        ResourceConsumerHandle optional = systemdHandle(
                "optional", "optional.service", true)
        ResourceConsumerHandle required = systemdHandle(
                "required", "required.service", false)
        ResourceControlCommand request = command(
                "APPLY", "0-1", null, [optional, required])
        request.sliceName = null

        ResourceControlResponse response = executor.apply(request)

        assert response.results*.state == ["SKIPPED", "ERROR"]
        assert response.expectedServiceCount == 1 &&
                response.coveredServiceCount == 0 :
                "optional absence must not dilute required-service coverage"
    }

    @Test
    void testUnavailableControllersAreObservableInsteadOfReportedReady() {
        ResourceControlCommand request = command(
                "APPLY", "0-1", null, [systemdHandle()])
        request.sliceName = null

        ResourceControlResponse response = executor.apply(request)
        List<ManagedServiceResourceUsage> usage = executor.inspect(
                "MANAGEMENT", [systemdHandle()])

        assert response.results*.state == ["ERROR"]
        assert response.expectedServiceCount == 1 &&
                response.coveredServiceCount == 0
        assert usage*.state == ["UNAVAILABLE"]
    }

    @Test
    void testInspectionFallsBackToMainPidWhenSystemdOmitsControlGroup() {
        configureV2(false)
        String pid = currentPid()
        String processGroupLine = new File("/proc/${pid}/cgroup").readLines()
                .find { it.startsWith("0::") }
        Assume.assumeTrue(
                "the MainPID fallback case requires a cgroup v2 process entry",
                processGroupLine != null)
        String processGroup = processGroupLine.substring(3)
        Path target = v2Root.resolve(
                processGroup.startsWith("/")
                        ? processGroup.substring(1) : processGroup)
        configureV2Group(target, "5", "0", null, 0)
        commands.unit("prometheus.service", true, "", pid)

        List<ManagedServiceResourceUsage> usage = executor.inspect(
                "MANAGEMENT", [systemdHandle()])

        assert usage[0].state == "RUNNING" && usage[0].cpuSet == "5" :
                "MainPID must keep usage observable when systemd returns no ControlGroup"
    }

    private void configureV2(boolean memory) {
        Files.createDirectories(v2Root)
        put(v2Root.resolve("cgroup.controllers"),
                memory ? "cpuset memory cpu" : "cpuset cpu")
        put(v2Root.resolve("cgroup.subtree_control"), "")
        put(v2Root.resolve("cpuset.cpus.effective"), "0-7")
        put(v2Root.resolve("cpuset.mems.effective"), "0")
        put(v2Root.resolve("cgroup.procs"), "")
        if (memory) {
            put(v2Root.resolve("memory.max"), "max")
            put(v2Root.resolve("memory.current"), "0")
        }
    }

    private Path configureV2SystemdRole(
            boolean serviceInSlice, boolean memory = true) {
        Path parent = v2Root.resolve("zstack.slice")
        configureV2Group(parent, "0-7", "0", memory ? "max" : null, 0)
        Path slice = v2Root.resolve(
                "zstack.slice/zstack-management.slice")
        configureV2Group(slice, "0-7", "0", memory ? "max" : null,
                SizeUnit.MEGABYTE.toByte(64))
        commands.unit("zstack-management.slice", true,
                "/zstack.slice/zstack-management.slice")
        Path service = serviceInSlice
                ? slice.resolve("prometheus.service")
                : v2Root.resolve("system.slice/prometheus.service")
        configureV2Group(service, "", "0", memory ? "max" : null,
                SizeUnit.MEGABYTE.toByte(32))
        put(service.resolve("cpu.stat"), "usage_usec 123\n")
        commands.unit("prometheus.service", true,
                "/${v2Root.relativize(service)}")
        return slice
    }

    private void configureV2Group(
            Path group, String cpus, String mems,
            String memoryLimit, long memoryCurrent) {
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
        Path slice = v1MemoryRoot.resolve(
                "zstack.slice/zstack-management.slice")
        configureV1MemoryGroup(slice, rootLimit,
                SizeUnit.MEGABYTE.toByte(64))
        configureV1MemoryGroup(slice.resolve("prometheus.service"),
                rootLimit, SizeUnit.MEGABYTE.toByte(24))
    }

    private void configureV1MemoryRoot(long rootLimit) {
        put(v1MemoryRoot.resolve("memory.limit_in_bytes"), "${rootLimit}")
        put(v1MemoryRoot.resolve("memory.usage_in_bytes"), "0")
        put(v1MemoryRoot.resolve("cgroup.procs"), "")
    }

    private ResourceConsumerHandle ownerHandle(String consumerKey) {
        Path pidFile = temporaryRoot.resolve("${consumerKey}.pid")
        put(pidFile, currentPid())
        Files.setPosixFilePermissions(pidFile, [
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.GROUP_READ,
                PosixFilePermission.OTHERS_READ
        ] as Set)
        return new ResourceConsumerHandle(
                ResourceConsumerHandle.OWNER_PID_FILE,
                pidFile.toString(), consumerKey, consumerKey,
                false, false, null)
    }

    private void configureV1MemoryGroup(
            Path group, long limit, long usage) {
        Files.createDirectories(group)
        put(group.resolve("memory.limit_in_bytes"), "${limit}")
        put(group.resolve("memory.usage_in_bytes"), "${usage}")
        put(group.resolve("cgroup.procs"), "")
    }

    private Path dropIn(String unit) {
        return systemdUnitRoot.resolve(unit + ".d")
                .resolve("50-zstack-resource-assignment.conf")
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

    private static ResourceControlCommand command(
            String operation,
            String cpuSet,
            Long memory,
            List<ResourceConsumerHandle> handles) {
        ResourceControlCommand command = new ResourceControlCommand()
        command.roleType = "MANAGEMENT"
        command.isolationMode = "SHARED"
        command.operation = operation
        command.cpuSet = cpuSet
        command.memory = memory
        command.sliceName = "zstack-management.slice"
        command.handles = handles
        return command
    }

    private static ResourceConsumerHandle systemdHandle(
            String serviceName = "prometheus",
            String unit = "prometheus.service",
            boolean optional = false) {
        return new ResourceConsumerHandle(
                ResourceConsumerHandle.SYSTEMD_UNIT,
                unit, serviceName, serviceName,
                optional, true, null)
    }

    private static String currentPid() {
        return ManagementFactory.runtimeMXBean.name.split("@")[0]
    }

    private static long memoryLimitAboveCurrentProcessUsage() {
        String rss = Files.readAllLines(
                java.nio.file.Paths.get("/proc/self/status"),
                StandardCharsets.US_ASCII).find { it.startsWith("VmRSS:") }
        assert rss != null :
                "this case requires /proc/self/status to expose VmRSS"
        long current = Long.parseLong(rss.trim().split(/\s+/)[1]) * 1024L
        long desired = Math.max(
                SizeUnit.GIGABYTE.toByte(4),
                current + SizeUnit.GIGABYTE.toByte(1))
        long mebibyte = SizeUnit.MEGABYTE.toByte(1)
        return Math.floorDiv(desired + mebibyte - 1, mebibyte) * mebibyte
    }

    private static Throwable capture(Closure action) {
        try {
            action.call()
            return null
        } catch (Throwable failure) {
            return failure
        }
    }

    private static void put(Path path, String value) {
        Files.createDirectories(path.parent)
        Files.write(path, value.getBytes(StandardCharsets.US_ASCII))
    }

    private static String text(Path path) {
        return new String(Files.readAllBytes(path), StandardCharsets.US_ASCII)
                .trim()
    }

    private static class FakeCommandExecutor
            implements LocalResourceControlExecutor.CommandExecutor {
        private final Path v2Root
        private final Path v1Root
        private final Path v1MemoryRoot
        private final Map<String, Map<String, String>> units = [:]
        private final List<List<String>> invocations = []
        private final Set<String> failStarts = [] as Set

        FakeCommandExecutor(Path v2Root, Path v1Root, Path v1MemoryRoot) {
            this.v2Root = v2Root
            this.v1Root = v1Root
            this.v1MemoryRoot = v1MemoryRoot
        }

        void unit(
                String name, boolean active, String controlGroup,
                String mainPid = "0") {
            units[name] = [
                    LoadState: "loaded",
                    ActiveState: active ? "active" : "inactive",
                    ControlGroup: controlGroup,
                    MainPID: mainPid
            ]
        }

        void missingUnit(String name) {
            units[name] = [
                    LoadState: "not-found",
                    ActiveState: "inactive",
                    ControlGroup: "",
                    MainPID: "0"
            ]
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
            if (args[0] == "mkdir" && args[1] == "-p") {
                Path path = java.nio.file.Paths.get(args[2])
                Files.createDirectories(path)
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
                Files.copy(source, destination,
                        StandardCopyOption.REPLACE_EXISTING)
                return ""
            }
            if (args[0] == "rm" && args[1] == "-f") {
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
                        LoadState: "not-found",
                        ActiveState: "inactive",
                        ControlGroup: "",
                        MainPID: "0"
                ]
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
                        units[unit] = [
                                LoadState: "loaded",
                                ActiveState: "inactive",
                                ControlGroup: "",
                                MainPID: "0"
                        ]
                    }
                    units[unit].ActiveState = action == "start" &&
                            !failStarts.contains(unit) ? "active" : "inactive"
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
                        LocalResourceControlExecutorFileSystemCase.text(
                                v1MemoryRoot.resolve("memory.limit_in_bytes")))
                putIfMissing(path.resolve("memory.usage_in_bytes"), "0")
                putIfMissing(path.resolve("cgroup.procs"), "")
            }
        }

        private void writeKernelFile(Path path, byte[] input) {
            String value = input == null ? "" :
                    new String(input, StandardCharsets.US_ASCII)
            if (path.fileName.toString() == "cgroup.subtree_control" &&
                    value.contains("+memory")) {
                path.parent.toFile().eachDir { File child ->
                    putIfMissing(child.toPath().resolve("memory.max"), "max")
                    putIfMissing(child.toPath().resolve("memory.current"), "0")
                }
            }
            if (path.fileName.toString() == "cgroup.procs") {
                Set<String> pids = [] as LinkedHashSet
                if (Files.isRegularFile(path)) {
                    pids.addAll(LocalResourceControlExecutorFileSystemCase
                            .text(path).split(/\s+/).findAll { it })
                }
                pids.addAll(value.trim().split(/\s+/).findAll { it })
                LocalResourceControlExecutorFileSystemCase.put(
                        path, pids.join("\n"))
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
            return command.size() >= 2 &&
                    command[0] == "sudo" && command[1] == "-n"
                    ? command.drop(2) : command
        }
    }
}
