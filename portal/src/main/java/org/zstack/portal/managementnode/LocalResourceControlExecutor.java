package org.zstack.portal.managementnode;

import org.zstack.core.CoreGlobalProperty;
import org.zstack.header.physicalserver.ManagedServiceResourceUsage;
import org.zstack.header.physicalserver.PhysicalServerCpuSet;
import org.zstack.header.physicalserver.PhysicalServerResourceIsolationMode;
import org.zstack.header.physicalserver.ResourceConsumerHandle;
import org.zstack.header.physicalserver.ResourceControlCommand;
import org.zstack.utils.data.SizeUnit;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class LocalResourceControlExecutor {
    private static final String SYSTEMD_DROP_IN = "50-zstack-resource-assignment.conf";
    private static final long COMMAND_TIMEOUT_SECONDS = 30;
    private static final int PROCESS_MOVE_ATTEMPTS = 3;

    private final AtomicInteger testCalls = new AtomicInteger();
    private volatile boolean testMode;
    private volatile ResourceControlCommand lastTestCommand;
    private final AtomicReference<List<ResourceConsumerHandle>>
            lastTestRestartHandles = new AtomicReference<>();
    private final ExecutionEnvironment environment;

    public LocalResourceControlExecutor() {
        this(ExecutionEnvironment.system());
        testMode = CoreGlobalProperty.UNIT_TEST_ON;
    }

    LocalResourceControlExecutor(ExecutionEnvironment environment) {
        this.environment = environment;
    }

    public boolean apply(ResourceControlCommand command) {
        if (CoreGlobalProperty.UNIT_TEST_ON && testMode) {
            return fakeApply(command);
        }

        PhysicalServerResourceIsolationMode isolationMode = isolationMode(command);
        String desired = normalizeOptional(command.getCpuSet());
        validateMemoryLimit(command.getMemory());
        if (isolationMode == PhysicalServerResourceIsolationMode.EXCLUSIVE && desired.isEmpty()) {
            throw new ResourceControlException("Exclusive isolation requires a CPU set");
        }
        if (desired.isEmpty() && command.getMemory() == null) {
            return true;
        }
        Backend backend;
        try {
            backend = backend();
        } catch (ResourceControlUnavailableException exception) {
            return false;
        }
        if (isolationMode == PhysicalServerResourceIsolationMode.EXCLUSIVE) {
            if (backend.version != CgroupVersion.V2) {
                throw new ResourceControlException("Exclusive CPU partitions require cgroup v2");
            }
        }
        Long desiredMemory = command.getMemory();
        Backend memoryBackend = null;
        if (desiredMemory != null) {
            try {
                memoryBackend = memoryBackend();
            } catch (MemoryControllerUnavailableException exception) {
            }
        }
        if (command.getSliceName() != null
                && command.getHandles().stream().anyMatch(handle ->
                ResourceConsumerHandle.SYSTEMD_UNIT.equals(handle.getHandleType()))) {
            return applySystemdSlice(command, backend, memoryBackend, desired, desiredMemory);
        }
        List<HandleControlResult> results = new ArrayList<>();
        for (ResourceConsumerHandle handle : command.getHandles()) {
            results.add(applyNonSystemdHandle(command, backend, memoryBackend, handle, desired, desiredMemory));
        }
        return summarizeApply(results, desired, desiredMemory);
    }

    public boolean release(ResourceControlCommand command) {
        if (CoreGlobalProperty.UNIT_TEST_ON && testMode) {
            return fakeRelease(command);
        }

        Backend backend;
        try {
            backend = backend();
        } catch (ResourceControlUnavailableException exception) {
            return false;
        }
        Backend memoryBackend = null;
        try {
            memoryBackend = memoryBackend();
        } catch (MemoryControllerUnavailableException exception) {
        }
        if (command.getSliceName() != null
                && command.getHandles().stream().anyMatch(handle ->
                ResourceConsumerHandle.SYSTEMD_UNIT.equals(handle.getHandleType()))) {
            return releaseSystemdSlice(command, backend, memoryBackend);
        }
        List<HandleControlResult> results = new ArrayList<>();
        for (ResourceConsumerHandle handle : command.getHandles()) {
            results.add(releaseNonSystemdHandle(command, backend, memoryBackend, handle));
        }
        return summarizeRelease(results);
    }

    private boolean applySystemdSlice(
            ResourceControlCommand command,
            Backend backend, Backend memoryBackend, String desired, Long desiredMemory) {
        boolean manageCpu = !desired.isEmpty();
        boolean memoryError = desiredMemory != null && memoryBackend == null;
        if (desiredMemory != null && desiredMemory > 0) {
            if (!memoryError) {
                try {
                    validateActiveSliceMemory(memoryBackend, command.getSliceName(), desiredMemory);
                } catch (SystemdControlGroupNotFoundException exception) {
                    memoryError = true;
                }
            }
        }
        boolean changed = pruneSystemdServiceDropIns(command.getSliceName(), command.getHandles());
        changed = configureSystemdSlice(
                backend, memoryBackend, command.getSliceName(), desired, desiredMemory) || changed;
        for (ResourceConsumerHandle handle : command.getHandles()) {
            if (ResourceConsumerHandle.SYSTEMD_UNIT.equals(handle.getHandleType())) {
                changed = configureSystemdService(handle, command.getSliceName()) || changed;
            }
        }
        boolean legacyCpuFallback = false;
        Map<Integer, HandleControlResult> legacyCpuResults = new HashMap<>();
        Path sliceTarget;
        try {
            sliceTarget = ensureActiveSliceTarget(backend, command.getSliceName());
        } catch (SystemdControlGroupNotFoundException exception) {
            if (isolationMode(command) == PhysicalServerResourceIsolationMode.EXCLUSIVE) {
                throw new ResourceControlException(String.format(
                        "Systemd slice[%s] must be active before applying exclusive isolation",
                        command.getSliceName()));
            }
            legacyCpuFallback = true;
            sliceTarget = null;
            if (manageCpu) {
                for (int index = 0; index < command.getHandles().size(); index++) {
                    ResourceConsumerHandle handle = command.getHandles().get(index);
                    if (!ResourceConsumerHandle.SYSTEMD_UNIT.equals(handle.getHandleType())) {
                        continue;
                    }
                    legacyCpuResults.put(index, applyNonSystemdHandle(command, backend, null, handle, desired, null));
                }
            } else {
                legacyCpuFallback = false;
            }
        }
        String actualCpuSet = null;
        Long actualMemory = null;
        Path memorySliceTarget = null;
        if (sliceTarget != null && manageCpu) {
            actualCpuSet = applyCpuBoundary(backend, sliceTarget, desired, isolationMode(command));
        }
        if (desiredMemory != null && memoryBackend != null) {
            try {
                memorySliceTarget = activeControllerSliceTarget(memoryBackend.root, command.getSliceName());
                if (memorySliceTarget != null) {
                    actualMemory = applyMemoryTarget(memoryBackend, memorySliceTarget, desiredMemory, null);
                }
            } catch (ResourceControlException exception) {
                memoryError = true;
            }
        }
        if (changed) {
            run(null, "sudo", "-n", "systemctl", "daemon-reload");
        }

        List<HandleControlResult> results = new ArrayList<>();
        for (int index = 0; index < command.getHandles().size(); index++) {
            ResourceConsumerHandle handle = command.getHandles().get(index);
            if (!ResourceConsumerHandle.SYSTEMD_UNIT.equals(handle.getHandleType())) {
                results.add(applyNonSystemdHandle(command, backend, memoryBackend, handle, desired, desiredMemory));
                continue;
            }
            Map<String, String> properties = systemdProperties(handle.getValue());
            if ("not-found".equals(properties.get("LoadState"))) {
                results.add(result(handle.isOptional() ? "SKIPPED" : "ERROR", null, null));
                continue;
            }
            if (!"active".equals(properties.get("ActiveState"))) {
                results.add(result(handle.isOptional() ? "SKIPPED" : "ERROR", null, null));
                continue;
            }
            String serviceCpuSet;
            if (legacyCpuFallback) {
                HandleControlResult cpuResult = legacyCpuResults.get(index);
                if (cpuResult == null || "ERROR".equals(cpuResult.getState())
                        || "SKIPPED".equals(cpuResult.getState())) {
                    results.add(cpuResult == null ? result("ERROR", null, null) : cpuResult);
                    continue;
                }
                serviceCpuSet = cpuResult.getCpuSet();
            } else {
                Path current;
                try {
                    current = systemdTarget(backend, properties.get("ControlGroup"));
                } catch (SystemdControlGroupException exception) {
                    results.add(result("PENDING_RESTART", null, null));
                    continue;
                }
                if (sliceTarget == null || !current.startsWith(sliceTarget)) {
                    results.add(result("PENDING_RESTART", null, null));
                    continue;
                }
                serviceCpuSet = actualCpuSet;
            }
            if (memoryError) {
                results.add(result("ERROR", null, null));
                continue;
            }
            if (command.getMemory() != null && memorySliceTarget != null
                    && !controlGroupInTarget(memoryBackend.root, properties.get("ControlGroup"), memorySliceTarget)) {
                results.add(result("PENDING_RESTART", null, null));
                continue;
            }
            results.add(result("READY", serviceCpuSet, actualMemory));
        }
        return summarizeApply(results, desired, desiredMemory);
    }

    private boolean releaseSystemdSlice(ResourceControlCommand command, Backend backend, Backend memoryBackend) {
        boolean changed = removeSystemdServiceDropIns(command.getSliceName());
        changed = removeDropIn(dropInPath(command.getSliceName())) || changed;
        for (ResourceConsumerHandle handle : command.getHandles()) {
            if (ResourceConsumerHandle.SYSTEMD_UNIT.equals(handle.getHandleType())) {
                changed = removeDropIn(dropInPath(handle.getValue())) || changed;
            }
        }

        Map<Integer, HandleControlResult> legacyResults = new HashMap<>();
        Path sliceTarget;
        try {
            sliceTarget = activeSliceTarget(backend, command.getSliceName());
        } catch (SystemdControlGroupNotFoundException exception) {
            sliceTarget = null;
            for (int index = 0; index < command.getHandles().size(); index++) {
                ResourceConsumerHandle handle = command.getHandles().get(index);
                if (ResourceConsumerHandle.SYSTEMD_UNIT.equals(handle.getHandleType())) {
                    legacyResults.put(index, releaseNonSystemdHandle(command, backend, null, handle));
                }
            }
        }

        boolean memoryError = false;
        if (sliceTarget != null) {
            releaseCpuBoundary(backend, sliceTarget);
        }
        if (memoryBackend != null) {
            try {
                Path memoryTarget = activeControllerSliceTarget(memoryBackend.root, command.getSliceName());
                if (memoryTarget != null) {
                    applyMemoryTarget(memoryBackend, memoryTarget, 0L, null);
                }
            } catch (ResourceControlException exception) {
                memoryError = true;
            }
        }
        if (changed) {
            run(null, "sudo", "-n", "systemctl", "daemon-reload");
        }

        List<HandleControlResult> results = new ArrayList<>();
        for (int index = 0; index < command.getHandles().size(); index++) {
            ResourceConsumerHandle handle = command.getHandles().get(index);
            if (!ResourceConsumerHandle.SYSTEMD_UNIT.equals(handle.getHandleType())) {
                results.add(releaseNonSystemdHandle(command, backend, memoryBackend, handle));
                continue;
            }
            Map<String, String> properties = systemdProperties(handle.getValue());
            if ("not-found".equals(properties.get("LoadState"))) {
                results.add(result(handle.isOptional() ? "SKIPPED" : "ERROR", null, null));
                continue;
            }
            if (!"active".equals(properties.get("ActiveState"))) {
                results.add(result(handle.isOptional() ? "SKIPPED" : "ERROR", null, null));
                continue;
            }
            HandleControlResult legacyResult = legacyResults.get(index);
            if (legacyResult != null && !"DISABLED".equals(legacyResult.getState())) {
                results.add(legacyResult);
                continue;
            }
            results.add(memoryError ? result("ERROR", null, null) : result("DISABLED", "", 0L));
        }
        return summarizeRelease(results);
    }

    private void validateActiveSliceMemory(Backend memoryBackend, String sliceName, long desiredMemory) {
        Map<String, String> properties = systemdProperties(sliceName);
        if (!"active".equals(properties.get("ActiveState"))) {
            return;
        }
        Path memoryTarget = systemdTarget(memoryBackend.root, properties.get("ControlGroup"));
        validateMemoryLimitAgainstUsage(
                memoryTarget.resolve(
                        memoryBackend.version == CgroupVersion.V2
                                ? "memory.current" : "memory.usage_in_bytes"), desiredMemory);
    }

    private HandleControlResult applyNonSystemdHandle(
            ResourceControlCommand command,
            Backend backend, Backend memoryBackend, ResourceConsumerHandle handle, String desired, Long desiredMemory) {
        try {
            Path target = resolve(backend, command.getRoleType(), handle);
            if (target == null) {
                return result("SKIPPED", null, null);
            }
            String actualCpuSet = !desired.isEmpty()
                    ? applyCpuBoundary(backend, target, desired, isolationMode(command)) : null;
            Long actualMemory = null;
            if (desiredMemory != null) {
                actualMemory = applyMemoryLimit(backend, memoryBackend, target, desiredMemory);
            }
            return result("READY", actualCpuSet, actualMemory);
        } catch (ResourceControlException exception) {
            return result("ERROR", null, null);
        }
    }

    private HandleControlResult releaseNonSystemdHandle(
            ResourceControlCommand command, Backend backend, Backend memoryBackend, ResourceConsumerHandle handle) {
        try {
            Path target = resolveForRelease(backend, command.getRoleType(), handle);
            if (target == null) {
                return result("SKIPPED", null, null);
            }
            releaseCpuBoundary(backend, target);
            Long actualMemory = 0L;
            if (memoryBackend != null) {
                actualMemory = applyMemoryLimit(backend, memoryBackend, target, 0L);
            }
            return result("DISABLED", "", actualMemory);
        } catch (ResourceControlException exception) {
            return result("ERROR", null, null);
        }
    }

    private boolean configureSystemdSlice(
            Backend backend, Backend memoryBackend, String sliceName, String cpuSet, Long memory) {
        Path path = dropInPath(sliceName);
        List<String> lines = new ArrayList<>();
        lines.add("[Slice]");
        if (backend.version == CgroupVersion.V2 && !cpuSet.isEmpty()) {
            lines.add("AllowedCPUs=" + cpuSet);
        }
        if (memory != null && memoryBackend != null) {
            lines.add(String.format(
                    "%s=%s",
                    memoryBackend.version == CgroupVersion.V2
                            ? "MemoryMax" : "MemoryLimit", memory == 0 ? "infinity" : memory));
        } else if (memory != null && Files.isRegularFile(path)) {
            for (String line : read(path).split("\\R")) {
                if (line.startsWith("MemoryMax=") || line.startsWith("MemoryLimit=")) {
                    lines.add(line);
                }
            }
        }
        return writeDropIn(path, String.join("\n", lines) + "\n");
    }

    private boolean configureSystemdService(ResourceConsumerHandle handle, String sliceName) {
        Path path = dropInPath(handle.getValue());
        return writeDropIn(path, "[Service]\nSlice=" + sliceName + "\n");
    }

    private boolean pruneSystemdServiceDropIns(String sliceName, List<ResourceConsumerHandle> handles) {
        Set<String> desiredUnits = new LinkedHashSet<>();
        for (ResourceConsumerHandle handle : handles) {
            if (ResourceConsumerHandle.SYSTEMD_UNIT.equals(handle.getHandleType())) {
                desiredUnits.add(handle.getValue());
            }
        }
        return removeSystemdServiceDropInsExcept(sliceName, desiredUnits);
    }

    private boolean removeSystemdServiceDropIns(String sliceName) {
        return removeSystemdServiceDropInsExcept(sliceName, Collections.emptySet());
    }

    private boolean removeSystemdServiceDropInsExcept(String sliceName, Set<String> desiredUnits) {
        if (!Files.isDirectory(environment.systemdUnitRoot, LinkOption.NOFOLLOW_LINKS)) {
            return false;
        }

        boolean changed = false;
        try (DirectoryStream<Path> directories = Files.newDirectoryStream(environment.systemdUnitRoot, "*.d")) {
            for (Path directory : directories) {
                if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) {
                    continue;
                }
                String name = directory.getFileName().toString();
                if (!name.endsWith(".d")) {
                    continue;
                }
                Path dropIn = directory.resolve(SYSTEMD_DROP_IN);
                if (!Files.isRegularFile(dropIn, LinkOption.NOFOLLOW_LINKS)
                        || !sliceName.equals(configuredSlice(dropIn))) {
                    continue;
                }
                String unit = name.substring(0, name.length() - 2);
                if (!desiredUnits.contains(unit)) {
                    changed = removeDropIn(dropIn) || changed;
                }
            }
        } catch (IOException exception) {
            throw new ResourceControlException("Failed to scan systemd service drop-ins: " + exception.getMessage());
        }
        return changed;
    }

    private Path dropInPath(String unit) {
        return environment.systemdUnitRoot.resolve(unit + ".d").resolve(SYSTEMD_DROP_IN);
    }

    private boolean writeDropIn(Path path, String content) {
        if (Files.isRegularFile(path) && content.equals(read(path))) {
            return false;
        }
        Path temporary = null;
        try {
            temporary = Files.createTempFile("zstack-resource-assignment-", ".conf");
            Files.write(temporary, content.getBytes(StandardCharsets.US_ASCII));
            run(null, "sudo", "-n", "mkdir", "-p", path.getParent().toString());
            run(null, "sudo", "-n", "install", "-m", "0644", temporary.toString(), path.toString());
            return true;
        } catch (IOException exception) {
            throw new ResourceControlException(String.format(
                    "Failed to write systemd drop-in[%s]: %s", path, exception.getMessage()));
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException ignored) {
                }
            }
        }
    }

    private boolean removeDropIn(Path path) {
        if (!Files.exists(path)) {
            return false;
        }
        run(null, "sudo", "-n", "rm", "-f", path.toString());
        return true;
    }

    private Path ensureActiveSliceTarget(Backend backend, String sliceName) {
        Map<String, String> properties = systemdProperties(sliceName);
        if (!"active".equals(properties.get("ActiveState"))) {
            run(null, "sudo", "-n", "systemctl", "start", sliceName);
        }
        return activeSliceTarget(backend, sliceName);
    }

    private Path activeSliceTarget(Backend backend, String sliceName) {
        Map<String, String> properties = systemdProperties(sliceName);
        if (!"active".equals(properties.get("ActiveState"))) {
            return null;
        }
        return systemdTarget(backend, properties.get("ControlGroup"));
    }

    private Path activeControllerSliceTarget(Path root, String sliceName) {
        Map<String, String> properties = systemdProperties(sliceName);
        if (!"active".equals(properties.get("ActiveState"))) {
            return null;
        }
        return systemdTarget(root, properties.get("ControlGroup"));
    }

    private Path systemdTarget(Backend backend, String controlGroup) {
        return systemdTarget(backend.root, controlGroup);
    }

    private Path systemdTarget(Path root, String controlGroup) {
        if (controlGroup == null || controlGroup.isEmpty()) {
            throw new SystemdControlGroupMissingException("Systemd did not report a control group");
        }
        Path target = underRoot(root, root.resolve(stripRoot(controlGroup)).normalize());
        if (target.equals(root) || !Files.isDirectory(target)) {
            throw new SystemdControlGroupNotFoundException(String.format(
                    "Systemd control group[%s] does not exist", controlGroup));
        }
        return target;
    }

    private boolean controlGroupInTarget(Path root, String controlGroup, Path target) {
        try {
            return systemdTarget(root, controlGroup).startsWith(target);
        } catch (ResourceControlException exception) {
            return false;
        }
    }

    public List<ManagedServiceResourceUsage> inspect(String roleType, List<ResourceConsumerHandle> handles) {
        if (CoreGlobalProperty.UNIT_TEST_ON && testMode) {
            return fakeInspect(handles);
        }
        Backend backend = backend();
        List<ManagedServiceResourceUsage> result = new ArrayList<>();
        Map<String, Path> sliceTargets = new HashMap<>();
        for (ResourceConsumerHandle handle : handles) {
            ServiceTarget target = inspectTarget(backend, roleType, handle);
            ManagedServiceResourceUsage usage = serviceUsage(handle, target.state);
            if (target.path != null) {
                usage.setRestartRequired(restartRequired(
                        backend, roleType, handle, target.state, target.path, sliceTargets));
                fillUsage(usage, backend, target.path, handle);
            }
            result.add(usage);
        }
        return result;
    }

    private boolean restartRequired(
            Backend backend,
            String roleType,
            ResourceConsumerHandle handle, String state, Path current, Map<String, Path> sliceTargets) {
        if (!"RUNNING".equals(state) || !ResourceConsumerHandle.SYSTEMD_UNIT.equals(handle.getHandleType())) {
            return false;
        }
        String sliceName = configuredSlice(handle.getValue());
        if (sliceName == null) {
            return false;
        }
        Path managed = backend.root.resolve(String.format(
                "zstack-role-%s-unit-%s", safeRole(roleType), safeRole(handle.getValue())));
        if (current.equals(managed) || groupHasProcesses(managed)) {
            return false;
        }
        Path sliceTarget = sliceTarget(backend, sliceName, sliceTargets);
        return sliceTarget == null || !current.startsWith(sliceTarget);
    }

    private String configuredSlice(String unit) {
        return configuredSlice(dropInPath(unit));
    }

    private String configuredSlice(Path path) {
        if (!Files.isRegularFile(path)) {
            return null;
        }
        try {
            for (String line : read(path).split("\\R")) {
                String value = line.trim();
                if (!value.startsWith("Slice=")) {
                    continue;
                }
                String slice = value.substring("Slice=".length()).trim();
                return slice.matches("[A-Za-z0-9_.@-]+\\.slice") ? slice : null;
            }
        } catch (ResourceControlException ignored) {
        }
        return null;
    }

    private Path sliceTarget(Backend backend, String sliceName, Map<String, Path> sliceTargets) {
        if (sliceTargets.containsKey(sliceName)) {
            return sliceTargets.get(sliceName);
        }
        Path target = null;
        try {
            Map<String, String> properties = systemdProperties(sliceName);
            if ("active".equals(properties.get("ActiveState"))) {
                target = systemdTarget(backend, properties.get("ControlGroup"));
            }
        } catch (ResourceControlException ignored) {
        }
        sliceTargets.put(sliceName, target);
        return target;
    }

    public void restart(String sliceName, List<ResourceConsumerHandle> handles) {
        if (handles == null || handles.isEmpty()) {
            throw new ResourceControlException("At least one service handle is required");
        }
        for (ResourceConsumerHandle handle : handles) {
            if (!ResourceConsumerHandle.SYSTEMD_UNIT.equals(handle.getHandleType()) || !handle.isRestartable()) {
                throw new ResourceControlException(String.format(
                        "Service[%s] is not a restartable systemd unit", handle.getServiceName()));
            }
        }
        if (CoreGlobalProperty.UNIT_TEST_ON && testMode) {
            lastTestRestartHandles.set(new ArrayList<>(handles));
            return;
        }

        List<String> units = new ArrayList<>();
        for (ResourceConsumerHandle handle : handles) {
            Map<String, String> properties = systemdProperties(handle.getValue());
            if ("not-found".equals(properties.get("LoadState"))) {
                throw new ResourceControlException(String.format("Systemd unit[%s] does not exist", handle.getValue()));
            }
            if (!"active".equals(properties.get("ActiveState"))) {
                throw new ResourceControlException(String.format("Systemd unit[%s] is not active", handle.getValue()));
            }
            if (!sliceName.equals(configuredSlice(handle.getValue()))) {
                throw new ResourceControlException(String.format(
                        "Systemd unit[%s] is not configured for slice[%s]", handle.getValue(), sliceName));
            }
            units.add(handle.getValue());
        }

        Backend backend = backend();
        Path sliceTarget;
        try {
            sliceTarget = activeSliceTarget(backend, sliceName);
        } catch (SystemdControlGroupNotFoundException exception) {
            sliceTarget = null;
        }
        if (backend.version == CgroupVersion.V2 && sliceTarget == null) {
            throw new ResourceControlException(String.format(
                    "Systemd slice[%s] is not active in the cpuset hierarchy", sliceName));
        }

        List<String> stop = new ArrayList<>(Arrays.asList("sudo", "-n", "systemctl", "stop"));
        stop.addAll(units);
        run(null, stop.toArray(new String[0]));
        List<String> start = new ArrayList<>(Arrays.asList("sudo", "-n", "systemctl", "start"));
        start.addAll(units);
        run(null, start.toArray(new String[0]));
        for (ResourceConsumerHandle handle : handles) {
            Map<String, String> properties = systemdProperties(handle.getValue());
            if (!"active".equals(properties.get("ActiveState"))) {
                throw new ResourceControlException(String.format(
                        "Systemd unit[%s] is not active after restart", handle.getValue()));
            }
            if (sliceTarget != null
                    && !controlGroupInTarget(backend.root, properties.get("ControlGroup"), sliceTarget)) {
                throw new ResourceControlException(String.format(
                        "Systemd unit[%s] did not enter slice[%s] after restart", handle.getValue(), sliceName));
            }
        }
    }

    private List<ManagedServiceResourceUsage> fakeInspect(List<ResourceConsumerHandle> handles) {
        List<ManagedServiceResourceUsage> result = new ArrayList<>();
        for (ResourceConsumerHandle handle : handles) {
            ManagedServiceResourceUsage usage = serviceUsage(handle, "RUNNING");
            if (lastTestCommand != null) {
                usage.setCpuSet(lastTestCommand.getCpuSet());
                usage.setMemoryLimit(lastTestCommand.getMemory());
            }
            result.add(usage);
        }
        return result;
    }

    private ManagedServiceResourceUsage serviceUsage(ResourceConsumerHandle handle, String state) {
        ManagedServiceResourceUsage usage = new ManagedServiceResourceUsage();
        usage.setServiceName(handle.getServiceName());
        usage.setRestartable(handle.isRestartable());
        usage.setRestartRequired(false);
        usage.setState(state);
        return usage;
    }

    private ServiceTarget inspectTarget(Backend backend, String roleType, ResourceConsumerHandle handle) {
        if (ResourceConsumerHandle.SYSTEMD_UNIT.equals(handle.getHandleType())) {
            Map<String, String> properties = systemdProperties(handle.getValue());
            if ("not-found".equals(properties.get("LoadState"))) {
                return new ServiceTarget("NOT_FOUND", null);
            }
            if (!"active".equals(properties.get("ActiveState"))) {
                return new ServiceTarget("INACTIVE", null);
            }
            String controlGroup = properties.get("ControlGroup");
            if (controlGroup != null && !controlGroup.isEmpty()) {
                Path current = underRoot(backend.root, backend.root.resolve(stripRoot(controlGroup)).normalize());
                if (Files.isDirectory(current)) {
                    return new ServiceTarget("RUNNING", current);
                }
            }
            Path managed = backend.root.resolve(String.format(
                    "zstack-role-%s-unit-%s", safeRole(roleType), safeRole(handle.getValue())));
            if (groupHasProcesses(managed)) {
                return new ServiceTarget("RUNNING", managed);
            }
            String mainPid = properties.get("MainPID");
            if (mainPid != null && mainPid.matches("[1-9][0-9]*")) {
                return new ServiceTarget("RUNNING", processGroup(backend, mainPid));
            }
            throw new SystemdControlGroupNotFoundException(String.format(
                    "No control group was found for systemd unit[%s]", handle.getValue()));
        }
        throw new ResourceControlException(String.format(
                "Resource consumer handle type[%s] is unsupported", handle.getHandleType()));
    }

    private boolean groupHasProcesses(Path target) {
        return Files.isDirectory(target)
                && Files.isRegularFile(target.resolve("cgroup.procs"))
                && !processIds(target.resolve("cgroup.procs")).isEmpty();
    }

    private void fillUsage(
            ManagedServiceResourceUsage usage, Backend backend, Path target, ResourceConsumerHandle handle) {
        usage.setCpuSet(effectiveCpuSet(backend, target));
        Path relative = backend.root.relativize(target);
        String controlGroup = null;
        if (ResourceConsumerHandle.SYSTEMD_UNIT.equals(handle.getHandleType())) {
            controlGroup = systemdProperties(handle.getValue()).get("ControlGroup");
        }
        usage.setCpuTime(cpuTime(relative, controlGroup));
        Backend memoryBackend;
        try {
            memoryBackend = memoryBackend();
        } catch (MemoryControllerUnavailableException exception) {
            return;
        }
        Path memoryTarget = controllerTarget(memoryBackend.root, relative);
        if (controlGroup != null && !controlGroup.isEmpty()) {
            Path current = controllerTarget(memoryBackend.root, Paths.get(stripRoot(controlGroup)));
            if (Files.isDirectory(current)) {
                memoryTarget = current;
            }
        }
        if (!Files.isDirectory(memoryTarget)) {
            return;
        }
        Path current = memoryTarget.resolve(
                memoryBackend.version == CgroupVersion.V2 ? "memory.current" : "memory.usage_in_bytes");
        usage.setMemory(Files.isRegularFile(current) ? parseMemoryLimit(read(current).trim()) : null);
        usage.setMemoryLimit(memoryBackend.version == CgroupVersion.V2
                ? effectiveV2MemoryLimit(memoryBackend.root, memoryTarget)
                : effectiveV1MemoryLimit(memoryBackend.root, memoryTarget));
    }

    private Long cpuTime(Path relative, String controlGroup) {
        if (!relative.toString().isEmpty()) {
            for (Path root : v2Roots()) {
                Long value = v2CpuTime(controllerTarget(root, relative));
                if (value != null) {
                    return value;
                }
            }
            Long value = v1CpuTime(relative);
            if (value != null) {
                return value;
            }
        }
        if (controlGroup == null || controlGroup.isEmpty()) {
            return null;
        }
        return v1CpuTime(Paths.get(stripRoot(controlGroup)));
    }

    private String effectiveCpuSet(Backend backend, Path target) {
        Path current = target;
        while (true) {
            Path effective = current.resolve("cpuset.cpus.effective");
            Path configured = current.resolve("cpuset.cpus");
            for (Path candidate : Arrays.asList(effective, configured)) {
                if (!Files.isRegularFile(candidate)) {
                    continue;
                }
                String value = normalizeOptional(read(candidate));
                if (!value.isEmpty()) {
                    return value;
                }
            }
            if (current.equals(backend.root)) {
                throw new ResourceControlException(String.format(
                        "Control group[%s] and its parents have no effective CPU set", target));
            }
            current = underRoot(backend.root, current.getParent());
        }
    }

    private Path processGroup(Backend backend, String pid) {
        for (String line : read(Paths.get("/proc", pid, "cgroup")).split("\\R")) {
            String[] fields = line.split(":", 3);
            if (fields.length != 3) {
                continue;
            }
            boolean matches = backend.version == CgroupVersion.V2 && "0".equals(fields[0]);
            if (backend.version == CgroupVersion.V1) {
                matches = Arrays.asList(fields[1].split(",")).contains("cpuset");
            }
            if (!matches) {
                continue;
            }
            Path target = underRoot(backend.root, backend.root.resolve(stripRoot(fields[2])).normalize());
            if (Files.isDirectory(target)) {
                return target;
            }
        }
        throw new ResourceControlException(String.format("No cpuset control group was found for process[%s]", pid));
    }

    private Long v2CpuTime(Path target) {
        Path stat = target.resolve("cpu.stat");
        if (!Files.isRegularFile(stat)) {
            return null;
        }
        for (String line : read(stat).split("\\R")) {
            String[] fields = line.trim().split("\\s+");
            if (fields.length == 2 && "usage_usec".equals(fields[0])) {
                return Math.multiplyExact(parseMemoryLimit(fields[1]), 1000L);
            }
        }
        return null;
    }

    private Long v1CpuTime(Path relative) {
        for (Path root : environment.v1CpuacctRoots) {
            Path usage = root.resolve(relative).resolve("cpuacct.usage");
            if (Files.isRegularFile(usage)) {
                return parseMemoryLimit(read(usage).trim());
            }
        }
        return null;
    }

    private Long effectiveV2MemoryLimit(Path root, Path target) {
        Long effective = null;
        Path current = target;
        while (true) {
            Path limit = current.resolve("memory.max");
            if (Files.isRegularFile(limit)) {
                String value = read(limit).trim();
                if (!"max".equals(value)) {
                    long parsed = parseMemoryLimit(value);
                    effective = effective == null ? parsed : Math.min(effective, parsed);
                }
            }
            if (current.equals(root)) {
                break;
            }
            current = underRoot(root, current.getParent());
        }
        return effective == null ? 0L : effective;
    }

    private Long effectiveV1MemoryLimit(Path root, Path target) {
        Path rootLimitPath = root.resolve("memory.limit_in_bytes");
        if (!Files.isRegularFile(rootLimitPath)) {
            return null;
        }
        long rootLimit = parseMemoryLimit(read(rootLimitPath).trim());
        underRoot(root, target);
        Long effective = null;
        Path current = target;
        while (true) {
            Path limit = current.resolve("memory.limit_in_bytes");
            if (Files.isRegularFile(limit)) {
                long parsed = parseMemoryLimit(read(limit).trim());
                effective = effective == null ? parsed : Math.min(effective, parsed);
            }
            if (current.equals(root)) {
                break;
            }
            current = underRoot(root, current.getParent());
        }
        if (effective == null) {
            return null;
        }
        return effective >= rootLimit ? 0L : effective;
    }

    public void enableTestMode() {
        if (!CoreGlobalProperty.UNIT_TEST_ON) {
            throw new IllegalStateException("test executor is only available in unit-test mode");
        }
        testMode = true;
        resetTestTracking();
    }

    public void disableTestMode() {
        if (!CoreGlobalProperty.UNIT_TEST_ON) {
            throw new IllegalStateException("test executor is only available in unit-test mode");
        }
        testMode = false;
        resetTestTracking();
    }

    private void resetTestTracking() {
        this.lastTestCommand = null;
        this.lastTestRestartHandles.set(null);
        this.testCalls.set(0);
    }

    public ResourceControlCommand getLastTestCommand() {
        return lastTestCommand;
    }

    public int getTestCalls() {
        return testCalls.get();
    }

    public List<ResourceConsumerHandle> getLastTestRestartHandles() {
        List<ResourceConsumerHandle> handles = lastTestRestartHandles.get();
        return handles == null ? null : new ArrayList<>(handles);
    }

    private boolean fakeApply(ResourceControlCommand command) {
        lastTestCommand = command;
        testCalls.incrementAndGet();
        String desiredCpuSet = normalizeOptional(command.getCpuSet());
        String actualCpuSet = desiredCpuSet.isEmpty() ? null : desiredCpuSet;
        Long actualMemory = null;
        if (command.getMemory() != null) {
            validateMemoryLimit(command.getMemory());
            actualMemory = command.getMemory();
        }
        List<HandleControlResult> results = new ArrayList<>();
        for (ResourceConsumerHandle handle : command.getHandles()) {
            results.add(result("READY", actualCpuSet, actualMemory));
        }
        return summarizeApply(results, desiredCpuSet, actualMemory);
    }

    private boolean fakeRelease(ResourceControlCommand command) {
        lastTestCommand = command;
        testCalls.incrementAndGet();
        List<HandleControlResult> results = new ArrayList<>();
        for (ResourceConsumerHandle handle : command.getHandles()) {
            results.add(result("DISABLED", "", 0L));
        }
        return summarizeRelease(results);
    }

    private boolean summarizeApply(List<HandleControlResult> results, String desiredCpuSet, Long desiredMemory) {
        int expected = 0;
        boolean synced = true;
        boolean manageCpu = !desiredCpuSet.isEmpty();

        for (HandleControlResult result : results) {
            if ("SKIPPED".equals(result.getState())) {
                continue;
            }
            expected++;
            if (!"READY".equals(result.getState())
                    || (manageCpu && !desiredCpuSet.equals(normalizeOptional(result.getCpuSet())))
                    || !memoryMatches(result.getMemory(), desiredMemory)) {
                synced = false;
            }
        }
        return expected > 0 && synced;
    }

    private boolean summarizeRelease(List<HandleControlResult> results) {
        for (HandleControlResult result : results) {
            if ("SKIPPED".equals(result.getState())) {
                continue;
            }
            if (!"DISABLED".equals(result.getState())
                    || !normalizeOptional(result.getCpuSet()).isEmpty() || !memoryMatches(result.getMemory(), 0L)) {
                return false;
            }
        }
        return true;
    }

    private boolean memoryMatches(Long actual, Long desired) {
        return desired == null || desired.equals(actual) || desired == 0L && actual == null;
    }

    private HandleControlResult result(String state, String cpuSet, Long memory) {
        HandleControlResult result = new HandleControlResult();
        result.setState(state);
        result.setCpuSet(cpuSet);
        result.setMemory(memory);
        return result;
    }

    private Backend backend() {
        for (Path root : v2Roots()) {
            Path controllers = root.resolve("cgroup.controllers");
            String values = read(controllers);
            if (values.matches("(?s).*\\bcpuset\\b.*") || Files.isRegularFile(root.resolve("cpuset.cpus.effective"))) {
                return new Backend(root, CgroupVersion.V2);
            }
        }
        if (Files.isRegularFile(environment.v1Root.resolve("cpuset.cpus"))) {
            return new Backend(environment.v1Root, CgroupVersion.V1);
        }
        throw new ResourceControlUnavailableException("No available cpuset controller was found");
    }

    private Backend memoryBackend() {
        for (Path root : v2Roots()) {
            String controllers = read(root.resolve("cgroup.controllers"));
            if (controllers.matches("(?s).*\\bmemory\\b.*") || Files.isRegularFile(root.resolve("memory.max"))) {
                return new Backend(root, CgroupVersion.V2);
            }
        }
        if (Files.isRegularFile(environment.v1MemoryRoot.resolve("memory.limit_in_bytes"))) {
            return new Backend(environment.v1MemoryRoot, CgroupVersion.V1);
        }
        throw new MemoryControllerUnavailableException("No available memory controller was found");
    }

    private List<Path> v2Roots() {
        Set<Path> roots = new LinkedHashSet<>();
        if (Files.isRegularFile(environment.v2Root.resolve("cgroup.controllers"))) {
            roots.add(environment.v2Root);
        }
        if (!Files.isRegularFile(environment.procMounts)) {
            return new ArrayList<>(roots);
        }
        for (String line : read(environment.procMounts).split("\\R")) {
            String[] fields = line.trim().split("\\s+");
            if (fields.length < 4 || !"cgroup2".equals(fields[2])
                    || !Arrays.asList(fields[3].split(",")).contains("rw")) {
                continue;
            }
            Path root = Paths.get(decodeMountPath(fields[1]));
            if (Files.isRegularFile(root.resolve("cgroup.controllers"))) {
                roots.add(root);
            }
        }
        return new ArrayList<>(roots);
    }

    private Path controllerTarget(Path root, Path relative) {
        return underRoot(root, root.resolve(relative).normalize());
    }

    private Path resolve(Backend backend, String roleType, ResourceConsumerHandle handle) {
        if (ResourceConsumerHandle.SYSTEMD_UNIT.equals(handle.getHandleType())) {
            return resolveSystemd(backend, roleType, handle);
        }
        throw new ResourceControlException(String.format(
                "Resource consumer handle type[%s] is unsupported", handle.getHandleType()));
    }

    private Path resolveSystemd(Backend backend, String roleType, ResourceConsumerHandle handle) {
        Path managedTarget = backend.root.resolve(String.format(
                "zstack-role-%s-unit-%s", safeRole(roleType), safeRole(handle.getValue())));
        Map<String, String> properties = systemdProperties(handle.getValue());
        if ("not-found".equals(properties.get("LoadState"))) {
            if (handle.isOptional()) {
                return null;
            }
            throw new ResourceControlException(String.format("Systemd unit[%s] does not exist", handle.getValue()));
        }
        if (!"active".equals(properties.get("ActiveState"))) {
            throw new ResourceControlException(String.format("Systemd unit[%s] is not active", handle.getValue()));
        }
        String controlGroup = properties.get("ControlGroup");
        if (controlGroup == null || controlGroup.isEmpty()) {
            throw new SystemdControlGroupMissingException(String.format(
                    "Systemd unit[%s] did not report a control group", handle.getValue()));
        }
        Path target = underRoot(backend.root, backend.root.resolve(stripRoot(controlGroup)).normalize());
        if (target.equals(backend.root)) {
            throw new ResourceControlException(String.format(
                    "Systemd unit[%s] reported the root control group", handle.getValue()));
        }
        if (!Files.isDirectory(target)) {
        return resolveSystemdV1Fallback(backend, handle, controlGroup, managedTarget);
        }
        return target;
    }

    private Path resolveForRelease(Backend backend, String roleType, ResourceConsumerHandle handle) {
        if (!ResourceConsumerHandle.SYSTEMD_UNIT.equals(handle.getHandleType())) {
            throw new ResourceControlException(String.format(
                    "Resource consumer handle type[%s] is unsupported", handle.getHandleType()));
        }
        Path managedTarget = backend.root.resolve(String.format(
                "zstack-role-%s-unit-%s", safeRole(roleType), safeRole(handle.getValue())));
        if (Files.isDirectory(managedTarget)) {
            return managedTarget;
        }
        Map<String, String> properties = systemdProperties(handle.getValue());
        if ("not-found".equals(properties.get("LoadState"))) {
            if (handle.isOptional()) {
                return null;
            }
            throw new ResourceControlException(String.format("Systemd unit[%s] does not exist", handle.getValue()));
        }
        if (!"active".equals(properties.get("ActiveState"))) {
            if (handle.isOptional()) {
                return null;
            }
            throw new ResourceControlException(String.format("Systemd unit[%s] is not active", handle.getValue()));
        }
        String controlGroup = properties.get("ControlGroup");
        if (controlGroup == null || controlGroup.isEmpty()) {
            throw new SystemdControlGroupMissingException(String.format(
                    "Systemd unit[%s] did not report a control group", handle.getValue()));
        }
        Path target = underRoot(backend.root, backend.root.resolve(stripRoot(controlGroup)).normalize());
        if (target.equals(backend.root)) {
            throw new ResourceControlException(String.format(
                    "Systemd unit[%s] reported the root control group", handle.getValue()));
        }
        return Files.isDirectory(target) ? target : null;
    }

    private Path resolveSystemdV1Fallback(
            Backend backend, ResourceConsumerHandle handle, String controlGroup, Path target) {
        Path source = underRoot(
                environment.v1SystemdRoot, environment.v1SystemdRoot.resolve(stripRoot(controlGroup)).normalize());
        Path sourceProcesses = source.resolve("cgroup.procs");
        if (!Files.isRegularFile(sourceProcesses)) {
            if (handle.isOptional()) {
                return null;
            }
            throw new SystemdControlGroupNotFoundException(String.format(
                    "Systemd control group[%s] does not exist", controlGroup));
        }
        if (processIds(sourceProcesses).isEmpty()) {
            if (handle.isOptional()) {
                return null;
            }
            throw new ResourceControlException(String.format(
                    "Systemd control group[%s] contains no processes", controlGroup));
        }
        mkdir(target);
        enableV2Path(backend, target);
        initializeMems(backend, target);
        initializeCpus(backend, target);
        Path targetProcesses = target.resolve("cgroup.procs");
        moveProcesses(
                sourceProcesses,
                targetProcesses,
                "Systemd control group process files are unavailable",
                "Some systemd unit processes could not be moved");
        return target;
    }

    private String applyToGroup(Backend backend, Path target, String desired) {
        enableV2Path(backend, target);
        initializeMems(backend, target);
        Path cpuFile = target.resolve("cpuset.cpus");
        if (!Files.isRegularFile(cpuFile)) {
            throw new ResourceControlException(String.format(
                    "Cpuset controller is not available for control group[%s]", target));
        }
        String configured = normalizeOptional(read(cpuFile));
        if (!configured.equals(desired)) {
            write(cpuFile, desired);
        }
        Path effective = target.resolve("cpuset.cpus.effective");
        return normalizeOptional(read(Files.isRegularFile(effective) ? effective : cpuFile));
    }

    private String applyCpuBoundary(
            Backend backend, Path target, String desired, PhysicalServerResourceIsolationMode isolationMode) {
        if (isolationMode != PhysicalServerResourceIsolationMode.EXCLUSIVE) {
            makePartitionMember(backend, target);
        }
        String actual = applyToGroup(backend, target, desired);
        if (isolationMode == PhysicalServerResourceIsolationMode.EXCLUSIVE) {
            makePartitionRoot(backend, target, desired);
            Path effective = target.resolve("cpuset.cpus.effective");
            actual = normalizeOptional(read(
                    Files.isRegularFile(effective) ? effective : target.resolve("cpuset.cpus")));
        }
        return actual;
    }

    private void releaseCpuBoundary(Backend backend, Path target) {
        makePartitionMember(backend, target);
        enableV2Path(backend, target);
        initializeMems(backend, target);
        Path cpuFile = target.resolve("cpuset.cpus");
        if (!Files.isRegularFile(cpuFile)) {
            throw new ResourceControlException(String.format(
                    "Cpuset controller is not available for control group[%s]", target));
        }
        String desired;
        if (backend.version == CgroupVersion.V2 && managedGroup(backend.root, target)) {
            moveProcessesToParent(target);
            desired = "";
        } else {
            desired = parentCpuSet(target);
        }
        if (!normalizeOptional(read(cpuFile)).equals(desired)) {
            write(cpuFile, desired.isEmpty() && backend.version == CgroupVersion.V2 ? "\n" : desired);
        }
        if (!normalizeOptional(read(cpuFile)).equals(desired)) {
            throw new ResourceControlException(String.format(
                    "Failed to release CPU set from control group[%s]", target));
        }
    }

    private void makePartitionRoot(Backend backend, Path target, String desired) {
        if (backend.version != CgroupVersion.V2) {
            throw new ResourceControlException("Exclusive CPU partitions require cgroup v2");
        }
        Path partition = target.resolve("cpuset.cpus.partition");
        if (!Files.isRegularFile(partition)) {
            throw new ResourceControlException(String.format(
                    "CPU partition interface is unavailable for control group[%s]", target));
        }
        Path exclusive = target.resolve("cpuset.cpus.exclusive");
        if (Files.isRegularFile(exclusive) && !desired.equals(normalizeOptional(read(exclusive)))) {
            write(exclusive, desired);
        }
        if (!"root".equals(read(partition).trim())) {
            write(partition, "root");
        }
        if (!"root".equals(read(partition).trim())) {
            throw new ResourceControlException(String.format(
                    "Failed to make control group[%s] an exclusive CPU partition", target));
        }
        Path effective = target.resolve("cpuset.cpus.exclusive.effective");
        if (Files.isRegularFile(effective) && !desired.equals(normalizeOptional(read(effective)))) {
            throw new ResourceControlException(String.format(
                    "Control group[%s] did not apply exclusive CPU set[%s]", target, desired));
        }
    }

    private void makePartitionMember(Backend backend, Path target) {
        if (backend.version != CgroupVersion.V2) {
            return;
        }
        Path partition = target.resolve("cpuset.cpus.partition");
        if (!Files.isRegularFile(partition)) {
            return;
        }
        if (!"member".equals(read(partition).trim())) {
            write(partition, "member");
        }
        if (!"member".equals(read(partition).trim())) {
            throw new ResourceControlException(String.format(
                    "Failed to return control group[%s] to the shared CPU partition", target));
        }
        Path exclusive = target.resolve("cpuset.cpus.exclusive");
        if (Files.isRegularFile(exclusive) && !normalizeOptional(read(exclusive)).isEmpty()) {
            write(exclusive, "\n");
        }
    }

    private PhysicalServerResourceIsolationMode isolationMode(ResourceControlCommand command) {
        return command.getIsolationMode() == null
                ? PhysicalServerResourceIsolationMode.SHARED : command.getIsolationMode();
    }

    private long applyMemoryLimit(Backend backend, Backend memoryBackend, Path cpuTarget, long desiredLimit) {
        if (memoryBackend == null) {
            throw new MemoryControllerUnavailableException(
                    "Memory controller is unavailable for the target control group");
        }
        Path relative = backend.root.relativize(cpuTarget);
        Path memoryTarget = controllerTarget(memoryBackend.root, relative);
        return applyMemoryTarget(memoryBackend, memoryTarget, desiredLimit, cpuTarget);
    }

    private long applyMemoryTarget(Backend memoryBackend, Path memoryTarget, long desiredLimit, Path cpuTarget) {
        boolean managed = managedGroup(memoryBackend.root, memoryTarget);
        if (memoryBackend.version == CgroupVersion.V2) {
            if (!Files.isDirectory(memoryTarget)) {
                if (managed && desiredLimit == 0) {
                    return 0L;
                }
                if (!managed || desiredLimit == 0) {
                    throw new MemoryControllerUnavailableException(String.format(
                            "Memory controller is unavailable for control group[%s]", memoryTarget));
                }
                mkdir(memoryTarget);
            }
            enableV2MemoryPath(memoryBackend, memoryTarget);
            Path limit = memoryTarget.resolve("memory.max");
            if (!Files.isRegularFile(limit)) {
                throw new MemoryControllerUnavailableException(String.format(
                        "Memory controller is unavailable for control group[%s]", memoryTarget));
            }
            if (managed && !memoryTarget.equals(cpuTarget) && desiredLimit > 0) {
                moveProcesses(cpuTarget.resolve("cgroup.procs"), memoryTarget.resolve("cgroup.procs"));
            }
            String desired = desiredLimit == 0 ? "max" : String.valueOf(desiredLimit);
            if (!desired.equals(read(limit).trim())) {
                validateMemoryLimitAgainstUsage(memoryTarget.resolve("memory.current"), desiredLimit);
                write(limit, desired);
            }
            String actual = read(limit).trim();
            if (!desired.equals(actual)) {
                throw new ResourceControlException(String.format(
                        "Control group[%s] did not apply memory limit[%s]", memoryTarget, desired));
            }
            if (managed && !memoryTarget.equals(cpuTarget) && desiredLimit == 0) {
                moveProcesses(memoryTarget.resolve("cgroup.procs"), memoryTarget.getParent().resolve("cgroup.procs"));
            }
            return "max".equals(actual) ? 0L : parseMemoryLimit(actual);
        }

        Path rootLimit = memoryBackend.root.resolve("memory.limit_in_bytes");
        if (!Files.isRegularFile(rootLimit)) {
            throw new MemoryControllerUnavailableException(
                    "Cgroup v1 memory controller does not expose its root limit");
        }
        if (!Files.isDirectory(memoryTarget)) {
            if (managed && desiredLimit == 0) {
                return 0L;
            }
            if (!managed || desiredLimit == 0) {
                throw new MemoryControllerUnavailableException(String.format(
                        "Memory controller is unavailable for control group[%s]", memoryTarget));
            }
            mkdir(memoryTarget);
        }
        Path limit = memoryTarget.resolve("memory.limit_in_bytes");
        if (!Files.isRegularFile(limit)) {
            throw new MemoryControllerUnavailableException(String.format(
                    "Memory controller is unavailable for control group[%s]", memoryTarget));
        }
        if (managed && desiredLimit > 0) {
            moveProcesses(cpuTarget.resolve("cgroup.procs"), memoryTarget.resolve("cgroup.procs"));
        }
        String desired = desiredLimit == 0 ? read(rootLimit).trim() : String.valueOf(desiredLimit);
        if (!desired.equals(read(limit).trim())) {
            validateMemoryLimitAgainstUsage(memoryTarget.resolve("memory.usage_in_bytes"), desiredLimit);
            write(limit, desired);
        }
        if (!desired.equals(read(limit).trim())) {
            throw new ResourceControlException(String.format(
                    "Control group[%s] did not apply memory limit[%s]", memoryTarget, desired));
        }
        if (managed && desiredLimit == 0) {
            moveProcesses(memoryTarget.resolve("cgroup.procs"), memoryTarget.getParent().resolve("cgroup.procs"));
        }
        return desiredLimit == 0 ? 0L : parseMemoryLimit(read(limit).trim());
    }

    private void enableV2MemoryPath(Backend backend, Path target) {
        if (Files.isRegularFile(target.resolve("memory.max"))) {
            return;
        }
        Path current = backend.root;
        for (Path part : backend.root.relativize(target)) {
            Path child = current.resolve(part);
            if (!Files.isRegularFile(child.resolve("memory.max"))) {
                Path controllers = current.resolve("cgroup.controllers");
                Path control = current.resolve("cgroup.subtree_control");
                if (!Files.isRegularFile(controllers)
                        || !read(controllers).matches("(?s).*\\bmemory\\b.*") || !Files.isRegularFile(control)) {
                    throw new MemoryControllerUnavailableException(String.format(
                            "Memory controller cannot be delegated below control group[%s]", current));
                }
                write(control, "+memory");
            }
            current = child;
        }
    }

    private void moveProcesses(Path source, Path destination) {
        moveProcesses(
                source,
                destination,
                "Memory controller process files are unavailable",
                "Some processes could not be moved to the memory control group");
    }

    private void moveProcessesToParent(Path target) {
        moveProcesses(
                target.resolve("cgroup.procs"),
                target.getParent().resolve("cgroup.procs"),
                "Cpuset controller process files are unavailable",
                "Some processes could not be moved to the parent control group");
    }

    private void moveProcesses(Path source, Path destination, String unavailableMessage, String mismatchMessage) {
        if (!Files.isRegularFile(source) || !Files.isRegularFile(destination)) {
            throw new ResourceControlException(unavailableMessage);
        }

        for (int attempt = 0; attempt < PROCESS_MOVE_ATTEMPTS; attempt++) {
            Set<String> destinationPids = processIds(destination);
            for (String pid : processIds(source)) {
                if (destinationPids.contains(pid) || !Files.isDirectory(Paths.get("/proc", pid))) {
                    continue;
                }
                try {
                    write(destination, pid);
                } catch (ResourceControlException exception) {
                    if (Files.isDirectory(Paths.get("/proc", pid))) {
                        throw exception;
                    }
                }
            }

            destinationPids = processIds(destination);
            boolean complete = true;
            for (String pid : processIds(source)) {
                if (Files.isDirectory(Paths.get("/proc", pid)) && !destinationPids.contains(pid)) {
                    complete = false;
                    break;
                }
            }
            if (complete) {
                return;
            }
        }
        throw new ResourceControlException(mismatchMessage);
    }

    private Set<String> processIds(Path source) {
        Set<String> pids = new LinkedHashSet<>();
        for (String pid : read(source).trim().split("\\s+")) {
            if (pid.matches("[1-9][0-9]*")) {
                pids.add(pid);
            }
        }
        return pids;
    }

    private boolean managedGroup(Path root, Path target) {
        Path relative = root.relativize(target);
        return relative.getNameCount() > 0 && relative.getName(0).toString().startsWith("zstack-role-");
    }

    private long parseMemoryLimit(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            throw new ResourceControlException(String.format("Memory value[%s] is not a valid byte count", value));
        }
    }

    private void validateMemoryLimitAgainstUsage(Path usage, long desiredLimit) {
        if (desiredLimit == 0) {
            return;
        }
        long current = Math.max(
                parseMemoryLimit(read(usage).trim()), residentMemoryUsage(usage.getParent().resolve("cgroup.procs")));
        if (desiredLimit < current) {
            throw new ResourceControlException(String.format(
                    "Memory limit[%s] is below current usage[%s]", desiredLimit, current));
        }
    }

    private long residentMemoryUsage(Path processFile) {
        if (!Files.isRegularFile(processFile)) {
            return 0;
        }
        long total = 0;
        for (String pid : processIds(processFile)) {
            Path process = Paths.get("/proc", pid);
            Path status = process.resolve("status");
            if (!Files.isRegularFile(status)) {
                continue;
            }
            String value;
            try {
                value = read(status);
            } catch (ResourceControlException exception) {
                if (!Files.isDirectory(process)) {
                    continue;
                }
                throw exception;
            }
            for (String line : value.split("\\R")) {
                if (!line.startsWith("VmRSS:")) {
                    continue;
                }
                String[] fields = line.trim().split("\\s+");
                if (fields.length < 2 || !fields[1].matches("[0-9]+")) {
                    throw new ResourceControlException(String.format(
                            "Process[%s] reported invalid resident memory usage[%s]", pid, line));
                }
                try {
                    total = Math.addExact(total, Math.multiplyExact(Long.parseLong(fields[1]), 1024L));
                } catch (NumberFormatException exception) {
                    throw new ResourceControlException(String.format(
                            "Process[%s] reported invalid resident memory usage[%s]", pid, fields[1]));
                } catch (ArithmeticException exception) {
                    return Long.MAX_VALUE;
                }
                break;
            }
        }
        return total;
    }

    private void validateMemoryLimit(Long value) {
        if (value == null) {
            return;
        }
        long mebibyte = SizeUnit.MEGABYTE.toByte(1);
        if (value < 0 || value % mebibyte != 0) {
            throw new ResourceControlException(String.format(
                    "Memory limit[%s] must be zero or a positive multiple of 1 MiB", value));
        }
    }

    private void enableV2Path(Backend backend, Path target) {
        if (backend.version == CgroupVersion.V1) {
            return;
        }
        Path current = backend.root;
        for (Path part : backend.root.relativize(target)) {
            Path child = current.resolve(part);
            if (!Files.isRegularFile(child.resolve("cpuset.cpus"))) {
                Path control = current.resolve("cgroup.subtree_control");
                if (!Files.isRegularFile(control)) {
                    throw new ResourceControlException(String.format(
                            "Cgroup v2 subtree control is unavailable for control group[%s]", current));
                }
                write(control, "+cpuset");
            }
            current = child;
        }
    }

    private void initializeMems(Backend backend, Path target) {
        Path mems = target.resolve("cpuset.mems");
        if (!Files.isRegularFile(mems) || !read(mems).trim().isEmpty()) {
            return;
        }
        Path parent = target.getParent();
        Path source = parent.resolve("cpuset.mems.effective");
        if (!Files.isRegularFile(source)) {
            source = parent.resolve("cpuset.mems");
        }
        String value = read(source).trim();
        if (value.isEmpty()) {
            throw new ResourceControlException(String.format(
                    "Control group[%s] has no effective memory node set", target.getParent()));
        }
        write(mems, value);
    }

    private void initializeCpus(Backend backend, Path target) {
        if (backend.version == CgroupVersion.V2) {
            return;
        }
        Path cpus = target.resolve("cpuset.cpus");
        if (read(cpus).trim().isEmpty()) {
            write(cpus, parentCpuSet(target));
        }
    }

    private String parentCpuSet(Path target) {
        Path parent = target.getParent();
        Path source = parent.resolve("cpuset.cpus.effective");
        if (!Files.isRegularFile(source)) {
            source = parent.resolve("cpuset.cpus");
        }
        String value = normalizeOptional(read(source));
        if (value.isEmpty()) {
            throw new ResourceControlException(String.format(
                    "Parent control group[%s] has no effective CPU set", parent));
        }
        return value;
    }

    private Map<String, String> systemdProperties(String unit) {
        String output = run(null,
                "systemctl", "show", unit,
                "--property=LoadState", "--property=ActiveState", "--property=ControlGroup", "--property=MainPID");
        Map<String, String> properties = new HashMap<>();
        for (String line : output.split("\\R")) {
            int separator = line.indexOf('=');
            if (separator > 0) {
                properties.put(line.substring(0, separator), line.substring(separator + 1));
            }
        }
        return properties;
    }

    private void mkdir(Path path) {
        if (!Files.isDirectory(path)) {
            run(null, "sudo", "-n", "mkdir", "-p", path.toString());
        }
        run(null, "sudo", "-n", "chmod", "0755", path.toString());
    }

    private String read(Path path) {
        try {
            return new String(Files.readAllBytes(path), StandardCharsets.US_ASCII);
        } catch (IOException exception) {
            throw new ResourceControlException(String.format(
                    "Failed to read file[%s]: %s", path, exception.getMessage()));
        }
    }

    private void write(Path path, String value) {
        run(value.getBytes(StandardCharsets.US_ASCII), "sudo", "-n", "tee", path.toString());
    }

    private String run(byte[] input, String... command) {
        if (environment.commandExecutor != null) {
            return environment.commandExecutor.run(input, command);
        }
        Process process = null;
        try {
            process = new ProcessBuilder(command).redirectErrorStream(true).start();
            if (input != null) {
                try (OutputStream stream = process.getOutputStream()) {
                    stream.write(input);
                }
            } else {
                process.getOutputStream().close();
            }
            if (!process.waitFor(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new ResourceControlException(String.format(
                        "Command[%s] timed out after %s seconds", command[0], COMMAND_TIMEOUT_SECONDS));
            }
            String output = readStream(process.getInputStream());
            if (process.exitValue() != 0) {
                throw new ResourceControlException(String.format("Command[%s] failed: %s", command[0], output.trim()));
            }
            return output;
        } catch (IOException exception) {
            throw new ResourceControlException(String.format(
                    "Failed to execute command[%s]: %s", command[0], exception.getMessage()));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResourceControlException(String.format("Command[%s] was interrupted", command[0]));
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    private String readStream(InputStream stream) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int count;
        while ((count = stream.read(buffer)) != -1) {
            output.write(buffer, 0, count);
        }
        return new String(output.toByteArray(), StandardCharsets.UTF_8);
    }

    private String normalizeOptional(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? "" : PhysicalServerCpuSet.normalize(trimmed);
    }

    private Path underRoot(Path root, Path path) {
        if (!path.equals(root) && !path.startsWith(root)) {
            throw new ResourceControlException(String.format("Control group path[%s] is outside root[%s]", path, root));
        }
        return path;
    }

    private String stripRoot(String path) {
        return path.startsWith("/") ? path.substring(1) : path;
    }

    private String decodeMountPath(String value) {
        return value.replace("\\040", " ").replace("\\011", "\t").replace("\\012", "\n").replace("\\134", "\\");
    }

    private String safeRole(String roleType) {
        String value = roleType == null ? "" : roleType.replaceAll("[^a-zA-Z0-9_.-]", "-");
        if (value.isEmpty()) {
            throw new ResourceControlException("Role type must contain at least one valid path character");
        }
        return value;
    }

    private static class HandleControlResult {
        private String state;
        private String cpuSet;
        private Long memory;

        private String getState() {
            return state;
        }

        private void setState(String state) {
            this.state = state;
        }

        private String getCpuSet() {
            return cpuSet;
        }

        private void setCpuSet(String cpuSet) {
            this.cpuSet = cpuSet;
        }

        private Long getMemory() {
            return memory;
        }

        private void setMemory(Long memory) {
            this.memory = memory;
        }
    }

    private static class Backend {
        private final Path root;
        private final CgroupVersion version;

        private Backend(Path root, CgroupVersion version) {
            this.root = root;
            this.version = version;
        }
    }

    private enum CgroupVersion {
        V1, V2
    }

    private static class ServiceTarget {
        private final String state;
        private final Path path;

        private ServiceTarget(String state, Path path) {
            this.state = state;
            this.path = path;
        }
    }

    interface CommandExecutor {
        String run(byte[] input, String... command);
    }

    static class ExecutionEnvironment {
        private final Path v2Root;
        private final Path v1Root;
        private final Path v1MemoryRoot;
        private final Path v1SystemdRoot;
        private final List<Path> v1CpuacctRoots;
        private final Path procMounts;
        private final Path systemdUnitRoot;
        private final CommandExecutor commandExecutor;

        ExecutionEnvironment(
                Path v2Root,
                Path v1Root,
                Path v1MemoryRoot,
                Path v1SystemdRoot,
                List<Path> v1CpuacctRoots, Path procMounts, Path systemdUnitRoot, CommandExecutor commandExecutor) {
            this.v2Root = v2Root;
            this.v1Root = v1Root;
            this.v1MemoryRoot = v1MemoryRoot;
            this.v1SystemdRoot = v1SystemdRoot;
            this.v1CpuacctRoots = new ArrayList<>(v1CpuacctRoots);
            this.procMounts = procMounts;
            this.systemdUnitRoot = systemdUnitRoot;
            this.commandExecutor = commandExecutor;
        }

        private static ExecutionEnvironment system() {
            return new ExecutionEnvironment(
                    Paths.get("/sys/fs/cgroup"),
                    Paths.get("/sys/fs/cgroup/cpuset"),
                    Paths.get("/sys/fs/cgroup/memory"),
                    Paths.get("/sys/fs/cgroup/systemd"),
                    Arrays.asList(Paths.get("/sys/fs/cgroup/cpu,cpuacct"), Paths.get("/sys/fs/cgroup/cpuacct")),
                    Paths.get("/proc/mounts"), Paths.get("/etc/systemd/system"), null);
        }
    }

    public static class ResourceControlException extends RuntimeException {
        public ResourceControlException(String message) {
            super(message);
        }
    }

    private static class ResourceControlUnavailableException extends ResourceControlException {
        private ResourceControlUnavailableException(String message) {
            super(message);
        }
    }

    private static class MemoryControllerUnavailableException extends ResourceControlException {
        private MemoryControllerUnavailableException(String message) {
            super(message);
        }
    }

    private abstract static class SystemdControlGroupException extends ResourceControlException {
        private SystemdControlGroupException(String message) {
            super(message);
        }
    }

    private static class SystemdControlGroupMissingException extends SystemdControlGroupException {
        private SystemdControlGroupMissingException(String message) {
            super(message);
        }
    }

    private static class SystemdControlGroupNotFoundException extends SystemdControlGroupException {
        private SystemdControlGroupNotFoundException(String message) {
            super(message);
        }
    }
}
