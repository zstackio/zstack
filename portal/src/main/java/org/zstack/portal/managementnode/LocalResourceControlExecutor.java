package org.zstack.portal.managementnode;

import org.zstack.core.CoreGlobalProperty;
import org.zstack.header.physicalserver.ManagedServiceResourceUsage;
import org.zstack.header.physicalserver.PhysicalServerCpuSet;
import org.zstack.header.physicalserver.ResourceConsumerHandle;
import org.zstack.header.physicalserver.ResourceControlCommand;
import org.zstack.header.physicalserver.ResourceControlResponse;
import org.zstack.header.physicalserver.ResourceControlResult;
import org.zstack.utils.data.SizeUnit;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Arrays;
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
    private static final String SYSTEMD_DROP_IN =
            "50-zstack-resource-assignment.conf";
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

    public ResourceControlResponse apply(ResourceControlCommand command) {
        if (CoreGlobalProperty.UNIT_TEST_ON && testMode) {
            return fakeApply(command);
        }

        ControlOperation operation = ControlOperation.from(command);
        Backend backend;
        try {
            backend = backend();
        } catch (ResourceControlException exception) {
            if ("RESOURCE_CONTROL_UNAVAILABLE".equals(errorType(exception))) {
                return unavailable(command);
            }
            throw exception;
        }
        String desired = operation == ControlOperation.APPLY
                ? PhysicalServerCpuSet.normalize(command.getCpuSet()) : "";
        validateMemoryLimit(command.getMemory());
        Long desiredMemory = command.getMemory() == null
                ? null : operation == ControlOperation.APPLY
                ? command.getMemory() : 0L;
        MemoryBackend memoryBackend = null;
        if (command.getMemory() != null) {
            try {
                memoryBackend = memoryBackend();
            } catch (ResourceControlException exception) {
                if (!"MEMORY_CONTROLLER_UNAVAILABLE".equals(
                        errorType(exception))) {
                    throw exception;
                }
            }
        }
        if (command.getSliceName() != null
                && command.getHandles().stream().anyMatch(handle ->
                ResourceConsumerHandle.SYSTEMD_UNIT.equals(
                        handle.getHandleType()))) {
            return applySystemdSlice(
                    command, backend, memoryBackend,
                    desired, desiredMemory, operation);
        }
        List<ResourceControlResult> results = new ArrayList<>();

        for (ResourceConsumerHandle handle : command.getHandles()) {
            Path target;
            try {
                target = resolve(
                        backend, command.getRoleType(), handle, operation);
            } catch (ResourceControlException exception) {
                results.add(result("ERROR", null, null));
                continue;
            }
            if (target == null) {
                results.add(result("SKIPPED", null, null));
                continue;
            }
            try {
                String actualCpuSet = applyToGroup(
                        backend, target, desired, operation);
                Long actualMemory = null;
                if (command.getMemory() != null) {
                    actualMemory = applyMemoryLimit(
                            backend, memoryBackend,
                            target, desiredMemory).actualLimit;
                }
                results.add(result(
                        operation.resultState,
                        actualCpuSet,
                        actualMemory));
            } catch (ResourceControlException exception) {
                results.add(result("ERROR", null, null));
            }
        }
        return summarize(results);
    }

    private ResourceControlResponse applySystemdSlice(
            ResourceControlCommand command,
            Backend backend,
            MemoryBackend memoryBackend,
            String desired,
            Long desiredMemory,
            ControlOperation operation) {
        boolean memoryError = command.getMemory() != null
                && memoryBackend == null;
        if (operation == ControlOperation.APPLY
                && desiredMemory != null && desiredMemory > 0) {
            if (!memoryError) {
                try {
                    validateActiveSliceMemory(
                            memoryBackend,
                            command.getSliceName(), desiredMemory);
                } catch (ResourceControlException exception) {
                    if (!"SYSTEMD_CONTROL_GROUP_NOT_FOUND".equals(
                            errorType(exception))) {
                        throw exception;
                    }
                    memoryError = true;
                }
            }
        }
        boolean changed = configureSystemdSlice(
                backend,
                memoryBackend,
                command.getSliceName(),
                desired,
                desiredMemory,
                operation);
        for (ResourceConsumerHandle handle : command.getHandles()) {
            if (ResourceConsumerHandle.SYSTEMD_UNIT.equals(
                    handle.getHandleType())) {
                changed = configureSystemdService(
                        handle, command.getSliceName(), operation) || changed;
            }
        }
        boolean legacyCpuFallback = false;
        Map<Integer, ResourceControlResult> legacyCpuResults = new HashMap<>();
        Path sliceTarget;
        try {
            sliceTarget = activeSliceTarget(
                    backend, command.getSliceName(), operation);
        } catch (ResourceControlException exception) {
            if (!"SYSTEMD_CONTROL_GROUP_NOT_FOUND".equals(
                    errorType(exception))) {
                throw exception;
            }
            legacyCpuFallback = true;
            sliceTarget = null;
            for (int index = 0; index < command.getHandles().size(); index++) {
                ResourceConsumerHandle handle = command.getHandles().get(index);
                if (!ResourceConsumerHandle.SYSTEMD_UNIT.equals(
                        handle.getHandleType())) {
                    continue;
                }
                legacyCpuResults.put(index, applyNonSystemdHandle(
                        command, backend, null, handle, desired,
                        null, operation));
            }
        }
        String actualCpuSet = "";
        Long actualMemory = null;
        Path memorySliceTarget = null;
        if (sliceTarget != null) {
            actualCpuSet = applyToGroup(
                    backend, sliceTarget, desired, operation);
        }
        if (command.getMemory() != null && !memoryError) {
            try {
                memorySliceTarget = activeControllerSliceTarget(
                        memoryBackend.root, command.getSliceName());
                if (memorySliceTarget != null) {
                    actualMemory = applyMemoryTarget(
                            memoryBackend, memorySliceTarget,
                            desiredMemory,
                            MemoryTargetMode.EXISTING,
                            null).actualLimit;
                }
            } catch (ResourceControlException exception) {
                memoryError = true;
            }
        }
        if (changed) {
            run(null, "sudo", "-n", "systemctl", "daemon-reload");
        }

        List<ResourceControlResult> results = new ArrayList<>();
        for (int index = 0; index < command.getHandles().size(); index++) {
            ResourceConsumerHandle handle = command.getHandles().get(index);
            if (!ResourceConsumerHandle.SYSTEMD_UNIT.equals(
                    handle.getHandleType())) {
                results.add(applyNonSystemdHandle(
                        command, backend, memoryBackend, handle, desired,
                        desiredMemory, operation));
                continue;
            }
            Map<String, String> properties = systemdProperties(handle.getValue());
            if ("not-found".equals(properties.get("LoadState"))) {
                results.add(result(
                        handle.isOptional() ? "SKIPPED" : "ERROR",
                        null, null));
                continue;
            }
            if (!"active".equals(properties.get("ActiveState"))) {
                results.add(result(
                        handle.isOptional() ? "SKIPPED" : "ERROR",
                        null, null));
                continue;
            }
            if (operation == ControlOperation.RELEASE) {
                results.add(memoryError
                        ? result("ERROR", null, null)
                        : result("DISABLED", "",
                                command.getMemory() == null ? null : 0L));
                continue;
            }
            String serviceCpuSet;
            if (legacyCpuFallback) {
                ResourceControlResult cpuResult = legacyCpuResults.get(index);
                if (cpuResult == null || "ERROR".equals(cpuResult.getState())
                        || "SKIPPED".equals(cpuResult.getState())) {
                    results.add(cpuResult == null
                            ? result("ERROR", null, null) : cpuResult);
                    continue;
                }
                serviceCpuSet = cpuResult.getCpuSet();
            } else {
                Path current;
                try {
                    current = systemdTarget(
                            backend, properties.get("ControlGroup"));
                } catch (ResourceControlException exception) {
                    String type = errorType(exception);
                    if (!"SYSTEMD_CONTROL_GROUP_MISSING".equals(type)
                            && !"SYSTEMD_CONTROL_GROUP_NOT_FOUND".equals(type)) {
                        throw exception;
                    }
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
                    && !controlGroupInTarget(
                            memoryBackend.root,
                            properties.get("ControlGroup"),
                            memorySliceTarget)) {
                results.add(result("PENDING_RESTART", null, null));
                continue;
            }
            results.add(result("READY", serviceCpuSet, actualMemory));
        }
        ResourceControlResponse response = summarize(results);
        if (sliceTarget != null) {
            response.setCpuSet(actualCpuSet);
        }
        if (command.getMemory() != null && !memoryError) {
            response.setMemory(actualMemory);
        }
        return response;
    }

    private void validateActiveSliceMemory(
            MemoryBackend memoryBackend,
            String sliceName,
            long desiredMemory) {
        Map<String, String> properties = systemdProperties(sliceName);
        if (!"active".equals(properties.get("ActiveState"))) {
            return;
        }
        Path memoryTarget = systemdTarget(
                memoryBackend.root, properties.get("ControlGroup"));
        validateMemoryLimitAgainstUsage(
                memoryTarget.resolve(
                        memoryBackend.version == CgroupVersion.V2
                                ? "memory.current"
                                : "memory.usage_in_bytes"),
                desiredMemory);
    }

    private ResourceControlResult applyNonSystemdHandle(
            ResourceControlCommand command,
            Backend backend,
            MemoryBackend memoryBackend,
            ResourceConsumerHandle handle,
            String desired,
            Long desiredMemory,
            ControlOperation operation) {
        try {
            Path target = resolve(
                    backend, command.getRoleType(), handle, operation);
            if (target == null) {
                return result("SKIPPED", null, null);
            }
            String actualCpuSet = applyToGroup(
                    backend, target, desired, operation);
            Long actualMemory = null;
            if (desiredMemory != null) {
                actualMemory = applyMemoryLimit(
                        backend, memoryBackend,
                        target, desiredMemory).actualLimit;
            }
            return result(
                    operation.resultState,
                    actualCpuSet, actualMemory);
        } catch (ResourceControlException exception) {
            return result("ERROR", null, null);
        }
    }

    private boolean configureSystemdSlice(
            Backend backend,
            MemoryBackend memoryBackend,
            String sliceName,
            String cpuSet,
            Long memory,
            ControlOperation operation) {
        Path path = dropInPath(sliceName);
        if (operation == ControlOperation.RELEASE) {
            return removeDropIn(path);
        }
        List<String> lines = new ArrayList<>();
        lines.add("[Slice]");
        if (backend.version == CgroupVersion.V2) {
            lines.add("AllowedCPUs=" + cpuSet);
        }
        if (memory != null && memoryBackend != null) {
            lines.add(String.format(
                    "%s=%s",
                    memoryBackend.version == CgroupVersion.V2
                            ? "MemoryMax" : "MemoryLimit",
                    memory == 0 ? "infinity" : memory));
        } else if (memory != null && Files.isRegularFile(path)) {
            for (String line : read(path).split("\\R")) {
                if (line.startsWith("MemoryMax=")
                        || line.startsWith("MemoryLimit=")) {
                    lines.add(line);
                }
            }
        }
        return writeDropIn(path, String.join("\n", lines) + "\n");
    }

    private boolean configureSystemdService(
            ResourceConsumerHandle handle,
            String sliceName,
            ControlOperation operation) {
        Path path = dropInPath(handle.getValue());
        if (operation == ControlOperation.RELEASE) {
            return removeDropIn(path);
        }
        return writeDropIn(
                path, "[Service]\nSlice=" + sliceName + "\n");
    }

    private Path dropInPath(String unit) {
        return environment.systemdUnitRoot.resolve(unit + ".d")
                .resolve(SYSTEMD_DROP_IN);
    }

    private boolean writeDropIn(Path path, String content) {
        if (Files.isRegularFile(path)
                && content.equals(read(path))) {
            return false;
        }
        Path temporary = null;
        try {
            temporary = Files.createTempFile(
                    "zstack-resource-assignment-", ".conf");
            Files.write(temporary, content.getBytes(StandardCharsets.US_ASCII));
            run(null, "sudo", "-n", "mkdir", "-p",
                    path.getParent().toString());
            run(null, "sudo", "-n", "install", "-m", "0644",
                    temporary.toString(), path.toString());
            return true;
        } catch (IOException exception) {
            throw new ResourceControlException(
                    "SYSTEMD_DROP_IN_WRITE_FAILED:" + exception.getMessage());
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

    private Path activeSliceTarget(
            Backend backend,
            String sliceName,
            ControlOperation operation) {
        Map<String, String> properties = systemdProperties(sliceName);
        if (operation == ControlOperation.APPLY
                && !"active".equals(properties.get("ActiveState"))) {
            run(null, "sudo", "-n", "systemctl", "start", sliceName);
            properties = systemdProperties(sliceName);
        }
        if (!"active".equals(properties.get("ActiveState"))) {
            return null;
        }
        return systemdTarget(backend, properties.get("ControlGroup"));
    }

    private Path activeControllerSliceTarget(
            Path root, String sliceName) {
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
            throw new ResourceControlException("SYSTEMD_CONTROL_GROUP_MISSING");
        }
        Path target = underRoot(
                root,
                root.resolve(stripRoot(controlGroup)).normalize());
        if (target.equals(root) || !Files.isDirectory(target)) {
            throw new ResourceControlException(
                    "SYSTEMD_CONTROL_GROUP_NOT_FOUND");
        }
        return target;
    }

    private boolean controlGroupInTarget(
            Path root, String controlGroup, Path target) {
        try {
            return systemdTarget(root, controlGroup).startsWith(target);
        } catch (ResourceControlException exception) {
            return false;
        }
    }

    private ResourceControlResponse unavailable(ResourceControlCommand command) {
        List<ResourceControlResult> results = new ArrayList<>();
        for (ResourceConsumerHandle handle : command.getHandles()) {
            results.add(result("ERROR", null, null));
        }
        return summarize(results);
    }

    public List<ManagedServiceResourceUsage> inspect(
            String roleType, List<ResourceConsumerHandle> handles) {
        if (CoreGlobalProperty.UNIT_TEST_ON && testMode) {
            return fakeInspect(handles);
        }
        Backend backend;
        try {
            backend = backend();
        } catch (ResourceControlException exception) {
            List<ManagedServiceResourceUsage> unavailable = new ArrayList<>();
            for (ResourceConsumerHandle handle : handles) {
                unavailable.add(serviceUsage(handle, "UNAVAILABLE"));
            }
            return unavailable;
        }
        List<ManagedServiceResourceUsage> result = new ArrayList<>();
        Map<String, Path> sliceTargets = new HashMap<>();
        for (ResourceConsumerHandle handle : handles) {
            ManagedServiceResourceUsage usage = serviceUsage(
                    handle, "UNAVAILABLE");
            try {
                ServiceTarget target = inspectTarget(
                        backend, roleType, handle);
                usage.setState(target.state);
                if (target.path != null) {
                    usage.setRestartRequired(restartRequired(
                            backend, roleType, handle,
                            target.state, target.path, sliceTargets));
                    fillUsage(usage, backend, target.path, handle);
                }
            } catch (ResourceControlException ignored) {
            }
            result.add(usage);
        }
        return result;
    }

    private boolean restartRequired(
            Backend backend,
            String roleType,
            ResourceConsumerHandle handle,
            String state,
            Path current,
            Map<String, Path> sliceTargets) {
        if (!"RUNNING".equals(state)
                || !ResourceConsumerHandle.SYSTEMD_UNIT.equals(
                handle.getHandleType())) {
            return false;
        }
        String sliceName = configuredSlice(handle.getValue());
        if (sliceName == null) {
            return false;
        }
        Path managed = backend.root.resolve(String.format(
                "zstack-role-%s-unit-%s",
                safeRole(roleType), safeRole(handle.getValue())));
        if (current.equals(managed) || groupHasProcesses(managed)) {
            return false;
        }
        Path sliceTarget = sliceTarget(
                backend, sliceName, sliceTargets);
        return sliceTarget == null || !current.startsWith(sliceTarget);
    }

    private String configuredSlice(String unit) {
        Path path = dropInPath(unit);
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
                return slice.matches("[A-Za-z0-9_.@-]+\\.slice")
                        ? slice : null;
            }
        } catch (ResourceControlException ignored) {
        }
        return null;
    }

    private Path sliceTarget(
            Backend backend,
            String sliceName,
            Map<String, Path> sliceTargets) {
        if (sliceTargets.containsKey(sliceName)) {
            return sliceTargets.get(sliceName);
        }
        Path target = null;
        try {
            Map<String, String> properties = systemdProperties(sliceName);
            if ("active".equals(properties.get("ActiveState"))) {
                target = systemdTarget(
                        backend, properties.get("ControlGroup"));
            }
        } catch (ResourceControlException ignored) {
        }
        sliceTargets.put(sliceName, target);
        return target;
    }

    public void restart(List<ResourceConsumerHandle> handles) {
        if (handles == null || handles.isEmpty()) {
            throw new ResourceControlException("SERVICE_HANDLE_SET_INVALID");
        }
        for (ResourceConsumerHandle handle : handles) {
            if (!ResourceConsumerHandle.SYSTEMD_UNIT.equals(
                    handle.getHandleType()) || !handle.isRestartable()) {
                throw new ResourceControlException(
                        "SERVICE_RESTART_NOT_ALLOWED");
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
                throw new ResourceControlException("SYSTEMD_UNIT_NOT_FOUND");
            }
            if (!"active".equals(properties.get("ActiveState"))) {
                throw new ResourceControlException("SYSTEMD_UNIT_NOT_ACTIVE");
            }
            units.add(handle.getValue());
        }
        List<String> stop = new ArrayList<>(Arrays.asList(
                "sudo", "-n", "systemctl", "stop"));
        stop.addAll(units);
        run(null, stop.toArray(new String[0]));
        List<String> start = new ArrayList<>(Arrays.asList(
                "sudo", "-n", "systemctl", "start"));
        start.addAll(units);
        run(null, start.toArray(new String[0]));
        for (ResourceConsumerHandle handle : handles) {
            if (!"active".equals(
                    systemdProperties(handle.getValue()).get("ActiveState"))) {
                throw new ResourceControlException(
                        "SYSTEMD_UNIT_RESTART_FAILED");
            }
        }
    }

    private List<ManagedServiceResourceUsage> fakeInspect(
            List<ResourceConsumerHandle> handles) {
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

    private ManagedServiceResourceUsage serviceUsage(
            ResourceConsumerHandle handle, String state) {
        ManagedServiceResourceUsage usage = new ManagedServiceResourceUsage();
        usage.setServiceName(handle.getServiceName());
        usage.setRestartable(handle.isRestartable());
        usage.setRestartRequired(false);
        usage.setState(state);
        return usage;
    }

    private ServiceTarget inspectTarget(
            Backend backend,
            String roleType,
            ResourceConsumerHandle handle) {
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
                Path current = underRoot(
                        backend.root,
                        backend.root.resolve(
                                stripRoot(controlGroup)).normalize());
                if (Files.isDirectory(current)) {
                    return new ServiceTarget("RUNNING", current);
                }
            }
            Path managed = backend.root.resolve(String.format(
                    "zstack-role-%s-unit-%s",
                    safeRole(roleType), safeRole(handle.getValue())));
            if (groupHasProcesses(managed)) {
                return new ServiceTarget("RUNNING", managed);
            }
            String mainPid = properties.get("MainPID");
            if (mainPid != null && mainPid.matches("[1-9][0-9]*")) {
                return new ServiceTarget(
                        "RUNNING", processGroup(backend, mainPid));
            }
            throw new ResourceControlException(
                    "SYSTEMD_CONTROL_GROUP_NOT_FOUND");
        }
        throw new ResourceControlException("HANDLE_TYPE_UNSUPPORTED");
    }

    private boolean groupHasProcesses(Path target) {
        return Files.isDirectory(target)
                && Files.isRegularFile(target.resolve("cgroup.procs"))
                && !processIds(target.resolve("cgroup.procs")).isEmpty();
    }

    private void fillUsage(
            ManagedServiceResourceUsage usage,
            Backend backend,
            Path target,
            ResourceConsumerHandle handle) {
        usage.setCpuSet(effectiveCpuSet(backend, target));
        Path relative = backend.root.relativize(target);
        String controlGroup = null;
        if (ResourceConsumerHandle.SYSTEMD_UNIT.equals(
                handle.getHandleType())) {
            controlGroup = systemdProperties(
                    handle.getValue()).get("ControlGroup");
        }
        usage.setCpuTime(cpuTime(relative, controlGroup));
        MemoryBackend memoryBackend;
        try {
            memoryBackend = memoryBackend();
        } catch (ResourceControlException exception) {
            if (!"MEMORY_CONTROLLER_UNAVAILABLE".equals(
                    errorType(exception))) {
                throw exception;
            }
            return;
        }
        Path memoryTarget = controllerTarget(memoryBackend.root, relative);
        if (controlGroup != null && !controlGroup.isEmpty()) {
            Path current = controllerTarget(
                    memoryBackend.root,
                    Paths.get(stripRoot(controlGroup)));
            if (Files.isDirectory(current)) {
                memoryTarget = current;
            }
        }
        if (!Files.isDirectory(memoryTarget)) {
            return;
        }
        Path current = memoryTarget.resolve(
                memoryBackend.version == CgroupVersion.V2
                        ? "memory.current" : "memory.usage_in_bytes");
        usage.setMemory(Files.isRegularFile(current)
                ? parseMemoryLimit(read(current).trim()) : null);
        usage.setMemoryLimit(memoryBackend.version == CgroupVersion.V2
                ? effectiveV2MemoryLimit(memoryBackend.root, memoryTarget)
                : effectiveV1MemoryLimit(
                        memoryBackend.root, memoryTarget));
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
                throw new ResourceControlException(
                        "RESOURCE_CONTROL_CPUSET_EMPTY");
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
            boolean matches = backend.version == CgroupVersion.V2
                    && "0".equals(fields[0]);
            if (backend.version == CgroupVersion.V1) {
                matches = Arrays.asList(fields[1].split(","))
                        .contains("cpuset");
            }
            if (!matches) {
                continue;
            }
            Path target = underRoot(
                    backend.root,
                    backend.root.resolve(stripRoot(fields[2])).normalize());
            if (Files.isDirectory(target)) {
                return target;
            }
        }
        throw new ResourceControlException(
                "PROCESS_CONTROL_GROUP_NOT_FOUND");
    }

    private Long v2CpuTime(Path target) {
        Path stat = target.resolve("cpu.stat");
        if (!Files.isRegularFile(stat)) {
            return null;
        }
        for (String line : read(stat).split("\\R")) {
            String[] fields = line.trim().split("\\s+");
            if (fields.length == 2 && "usage_usec".equals(fields[0])) {
                return Math.multiplyExact(
                        parseMemoryLimit(fields[1]), 1000L);
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
                    effective = effective == null
                            ? parsed : Math.min(effective, parsed);
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
                effective = effective == null
                        ? parsed : Math.min(effective, parsed);
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

    private ResourceControlResponse fakeApply(ResourceControlCommand command) {
        lastTestCommand = command;
        testCalls.incrementAndGet();
        ControlOperation operation = ControlOperation.from(command);
        String actualCpuSet = operation == ControlOperation.APPLY
                ? PhysicalServerCpuSet.normalize(command.getCpuSet()) : "";
        Long actualMemory = null;
        if (command.getMemory() != null) {
            validateMemoryLimit(command.getMemory());
            actualMemory = operation == ControlOperation.APPLY
                    ? command.getMemory() : 0L;
        }
        List<ResourceControlResult> results = new ArrayList<>();
        for (ResourceConsumerHandle handle : command.getHandles()) {
            results.add(result(
                    operation.resultState,
                    actualCpuSet,
                    actualMemory));
        }
        return summarize(results);
    }

    private ResourceControlResponse summarize(List<ResourceControlResult> results) {
        int expected = 0;
        int covered = 0;
        Set<String> actualSets = new LinkedHashSet<>();
        Set<Long> actualMemory = new LinkedHashSet<>();

        for (ResourceControlResult result : results) {
            if ("SKIPPED".equals(result.getState())) {
                continue;
            }
            expected++;
            if ("READY".equals(result.getState())
                    || "DISABLED".equals(result.getState())) {
                covered++;
                if (result.getCpuSet() != null) {
                    actualSets.add(result.getCpuSet());
                }
                if (result.getMemory() != null) {
                    actualMemory.add(result.getMemory());
                }
            }
        }

        String cpuSet = actualSets.size() == 1
                ? actualSets.iterator().next() : "";
        Long memory = actualMemory.size() == 1
                ? actualMemory.iterator().next() : null;

        ResourceControlResponse response = new ResourceControlResponse();
        response.setCpuSet(cpuSet);
        response.setMemory(memory);
        response.setCoveredServiceCount(covered);
        response.setExpectedServiceCount(expected);
        response.setResults(results);
        return response;
    }

    private ResourceControlResult result(
            String state,
            String cpuSet,
            Long memory) {
        ResourceControlResult result = new ResourceControlResult();
        result.setState(state);
        result.setCpuSet(cpuSet);
        result.setMemory(memory);
        return result;
    }

    private Backend backend() {
        for (Path root : v2Roots()) {
            Path controllers = root.resolve("cgroup.controllers");
            String values = read(controllers);
            if (values.matches("(?s).*\\bcpuset\\b.*")
                    || Files.isRegularFile(
                            root.resolve("cpuset.cpus.effective"))) {
                return new Backend(
                        "CGROUP_V2_CPUSET", root, CgroupVersion.V2);
            }
        }
        if (Files.isRegularFile(
                environment.v1Root.resolve("cpuset.cpus"))) {
            return new Backend(
                    "CGROUP_V1_CPUSET",
                    environment.v1Root,
                    CgroupVersion.V1);
        }
        throw new ResourceControlException("RESOURCE_CONTROL_UNAVAILABLE");
    }

    private MemoryBackend memoryBackend() {
        for (Path root : v2Roots()) {
            String controllers = read(root.resolve("cgroup.controllers"));
            if (controllers.matches("(?s).*\\bmemory\\b.*")
                    || Files.isRegularFile(root.resolve("memory.max"))) {
                return new MemoryBackend(
                        "CGROUP_V2_MEMORY", root, CgroupVersion.V2);
            }
        }
        if (Files.isRegularFile(
                environment.v1MemoryRoot.resolve("memory.limit_in_bytes"))) {
            return new MemoryBackend(
                    "CGROUP_V1_MEMORY",
                    environment.v1MemoryRoot,
                    CgroupVersion.V1);
        }
        throw new ResourceControlException("MEMORY_CONTROLLER_UNAVAILABLE");
    }

    private List<Path> v2Roots() {
        Set<Path> roots = new LinkedHashSet<>();
        if (Files.isRegularFile(
                environment.v2Root.resolve("cgroup.controllers"))) {
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

    private Path resolve(
            Backend backend,
            String roleType,
            ResourceConsumerHandle handle,
            ControlOperation operation) {
        if (ResourceConsumerHandle.SYSTEMD_UNIT.equals(handle.getHandleType())) {
            return resolveSystemd(backend, roleType, handle, operation);
        }
        if (ResourceConsumerHandle.OWNER_PID_FILE.equals(handle.getHandleType())) {
            return resolvePidFile(backend, roleType, handle, operation);
        }
        throw new ResourceControlException("HANDLE_TYPE_UNSUPPORTED");
    }

    private Path resolveSystemd(
            Backend backend,
            String roleType,
            ResourceConsumerHandle handle,
            ControlOperation operation) {
        Path managedTarget = backend.root.resolve(String.format(
                "zstack-role-%s-unit-%s",
                safeRole(roleType), safeRole(handle.getValue())));
        if (operation == ControlOperation.RELEASE
                && Files.isDirectory(managedTarget)) {
            return managedTarget;
        }
        Map<String, String> properties = systemdProperties(handle.getValue());
        if ("not-found".equals(properties.get("LoadState"))) {
            if (handle.isOptional()) {
                return null;
            }
            throw new ResourceControlException("SYSTEMD_UNIT_NOT_FOUND");
        }
        if (!"active".equals(properties.get("ActiveState"))) {
            if (operation == ControlOperation.RELEASE
                    && handle.isOptional()) {
                return null;
            }
            throw new ResourceControlException("SYSTEMD_UNIT_NOT_ACTIVE");
        }
        String controlGroup = properties.get("ControlGroup");
        if (controlGroup == null || controlGroup.isEmpty()) {
            throw new ResourceControlException("SYSTEMD_CONTROL_GROUP_MISSING");
        }
        Path target = underRoot(backend.root, backend.root.resolve(stripRoot(controlGroup)).normalize());
        if (target.equals(backend.root)) {
            throw new ResourceControlException("SYSTEMD_CONTROL_GROUP_INVALID");
        }
        if (!Files.isDirectory(target)) {
            return resolveSystemdV1Fallback(
                    backend, handle, controlGroup, operation, managedTarget);
        }
        return target;
    }

    private Path resolveSystemdV1Fallback(
            Backend backend,
            ResourceConsumerHandle handle,
            String controlGroup,
            ControlOperation operation,
            Path target) {
        if (operation == ControlOperation.RELEASE) {
            return null;
        }
        Path source = underRoot(
                environment.v1SystemdRoot,
                environment.v1SystemdRoot.resolve(
                        stripRoot(controlGroup)).normalize());
        Path sourceProcesses = source.resolve("cgroup.procs");
        if (!Files.isRegularFile(sourceProcesses)) {
            if (handle.isOptional()) {
                return null;
            }
            throw new ResourceControlException("SYSTEMD_CONTROL_GROUP_NOT_FOUND");
        }
        if (processIds(sourceProcesses).isEmpty()) {
            if (handle.isOptional()) {
                return null;
            }
            throw new ResourceControlException("SYSTEMD_CONTROL_GROUP_EMPTY");
        }
        mkdir(target);
        enableV2Path(backend, target);
        initializeMems(backend, target);
        initializeCpus(backend, target);
        Path targetProcesses = target.resolve("cgroup.procs");
        moveProcesses(
                sourceProcesses,
                targetProcesses,
                "SYSTEMD_CONTROL_GROUP_NOT_FOUND",
                "SYSTEMD_PROCESS_MOVE_FAILED");
        return target;
    }

    private Path resolvePidFile(
            Backend backend,
            String roleType,
            ResourceConsumerHandle handle,
            ControlOperation operation) {
        Path target = backend.root.resolve(String.format(
                "zstack-role-%s-owner-%s",
                safeRole(roleType),
                safeRole(handle.getConsumerKey() == null
                        ? handle.getValue() : handle.getConsumerKey())));
        if (operation == ControlOperation.RELEASE) {
            return Files.isDirectory(target) ? target : null;
        }
        Path pidFile = Paths.get(handle.getValue());
        if (!Files.exists(pidFile, LinkOption.NOFOLLOW_LINKS)) {
            if (handle.isOptional()) {
                return null;
            }
            throw new ResourceControlException("PID_FILE_NOT_FOUND");
        }
        try {
            PosixFileAttributes attributes = Files.readAttributes(
                    pidFile, PosixFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            if (!attributes.isRegularFile() || Files.isSymbolicLink(pidFile)) {
                throw new ResourceControlException("PID_FILE_NOT_REGULAR");
            }
            String content = read(pidFile).trim();
            if (!content.matches("[1-9][0-9]*")) {
                throw new ResourceControlException("PID_FILE_FORMAT_INVALID");
            }
            long parsed = Long.parseLong(content);
            if (parsed > Integer.MAX_VALUE) {
                throw new ResourceControlException("PID_FILE_FORMAT_INVALID");
            }
            int pid = (int) parsed;
            Path proc = Paths.get("/proc", String.valueOf(pid));
            if (!Files.isDirectory(proc)) {
                throw new ResourceControlException("PID_NOT_RUNNING");
            }
            if (attributes.lastModifiedTime().toMillis() + 1000 < processStartTimeMillis(pid)) {
                throw new ResourceControlException("PID_FILE_STALE");
            }
            PosixFileAttributes procAttributes = Files.readAttributes(
                    proc, PosixFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            String expected = handle.getExpectedCommandToken();
            Set<PosixFilePermission> permissions = attributes.permissions();
            boolean writableByOthers = permissions.contains(PosixFilePermission.GROUP_WRITE)
                    || permissions.contains(PosixFilePermission.OTHERS_WRITE);
            boolean trustedRootPidFile = "root".equals(attributes.owner().getName())
                    && "root".equals(procAttributes.owner().getName())
                    && expected != null && !expected.isEmpty();
            if (writableByOthers && !trustedRootPidFile) {
                throw new ResourceControlException("PID_FILE_PERMISSION_INVALID");
            }
            if (!"root".equals(attributes.owner().getName())
                    && !attributes.owner().equals(procAttributes.owner())) {
                throw new ResourceControlException("PID_FILE_OWNER_MISMATCH");
            }
            String command = new String(Files.readAllBytes(proc.resolve("cmdline")), StandardCharsets.UTF_8)
                    .replace('\0', ' ');
            if (expected != null && !command.contains(expected)) {
                throw new ResourceControlException("PID_COMMAND_MISMATCH");
            }

            mkdir(target);
            enableV2Path(backend, target);
            initializeMems(backend, target);
            initializeCpus(backend, target);
            write(target.resolve("cgroup.procs"), String.valueOf(pid));
            if (!processIds(target.resolve("cgroup.procs")).contains(String.valueOf(pid))) {
                throw new ResourceControlException("PID_PROCESS_MOVE_FAILED");
            }
            return target;
        } catch (NumberFormatException exception) {
            throw new ResourceControlException("PID_FILE_FORMAT_INVALID");
        } catch (IOException exception) {
            throw new ResourceControlException("PID_FILE_READ_FAILED:" + exception.getMessage());
        }
    }

    private long processStartTimeMillis(int pid) {
        long bootTimeSeconds = -1;
        for (String line : read(Paths.get("/proc/stat")).split("\\R")) {
            if (line.startsWith("btime ")) {
                try {
                    bootTimeSeconds = Long.parseLong(line.substring("btime ".length()).trim());
                } catch (NumberFormatException exception) {
                    throw new ResourceControlException("PID_START_TIME_UNAVAILABLE");
                }
                break;
            }
        }
        String stat = read(Paths.get("/proc", String.valueOf(pid), "stat"));
        int commandEnd = stat.lastIndexOf(')');
        if (bootTimeSeconds < 0 || commandEnd < 0) {
            throw new ResourceControlException("PID_START_TIME_UNAVAILABLE");
        }
        String[] fields = stat.substring(commandEnd + 1).trim().split("\\s+");
        if (fields.length <= 19) {
            throw new ResourceControlException("PID_START_TIME_UNAVAILABLE");
        }
        try {
            long startTicks = Long.parseLong(fields[19]);
            long clockTicks = Long.parseLong(run(null, "getconf", "CLK_TCK").trim());
            if (clockTicks <= 0) {
                throw new ResourceControlException("PID_START_TIME_UNAVAILABLE");
            }
            return bootTimeSeconds * 1000 + startTicks * 1000 / clockTicks;
        } catch (NumberFormatException exception) {
            throw new ResourceControlException("PID_START_TIME_UNAVAILABLE");
        }
    }

    private String applyToGroup(
            Backend backend,
            Path target,
            String desired,
            ControlOperation operation) {
        enableV2Path(backend, target);
        initializeMems(backend, target);
        Path cpuFile = target.resolve("cpuset.cpus");
        if (!Files.isRegularFile(cpuFile)) {
            throw new ResourceControlException("CPUSET_CONTROLLER_NOT_DELEGATED");
        }
        String value = desired;
        if (operation == ControlOperation.RELEASE) {
            if (backend.version == CgroupVersion.V2
                    && managedGroup(backend.root, target)) {
                moveProcessesToParent(target);
                value = "";
            } else {
                value = parentCpuSet(target);
            }
        }
        String configured = normalizeOptional(read(cpuFile));
        if (!configured.equals(value)) {
            write(cpuFile,
                    value.isEmpty() && backend.version == CgroupVersion.V2
                            ? "\n" : value);
        }
        if (operation == ControlOperation.RELEASE) {
            if (!normalizeOptional(read(cpuFile)).equals(value)) {
                throw new ResourceControlException("CPUSET_RELEASE_MISMATCH");
            }
            return "";
        }
        Path effective = target.resolve("cpuset.cpus.effective");
        return normalizeOptional(read(Files.isRegularFile(effective) ? effective : cpuFile));
    }

    private MemoryResult applyMemoryLimit(
            Backend backend,
            MemoryBackend memoryBackend,
            Path cpuTarget,
            long desiredLimit) {
        if (memoryBackend == null) {
            throw new ResourceControlException(
                    "MEMORY_CONTROLLER_UNAVAILABLE");
        }
        Path relative = backend.root.relativize(cpuTarget);
        Path memoryTarget = controllerTarget(memoryBackend.root, relative);
        boolean managed = managedGroup(backend.root, cpuTarget);
        return applyMemoryTarget(
                memoryBackend,
                memoryTarget,
                desiredLimit,
                managed
                        ? MemoryTargetMode.MANAGED
                        : MemoryTargetMode.EXISTING,
                cpuTarget);
    }

    private MemoryResult applyMemoryTarget(
            MemoryBackend memoryBackend,
            Path memoryTarget,
            long desiredLimit,
            MemoryTargetMode targetMode,
            Path cpuTarget) {
        if (memoryBackend.version == CgroupVersion.V2) {
            if (!Files.isDirectory(memoryTarget)) {
                if (targetMode == MemoryTargetMode.EXISTING
                        || desiredLimit == 0) {
                    throw new ResourceControlException(
                            "MEMORY_CONTROLLER_UNAVAILABLE");
                }
                mkdir(memoryTarget);
            }
            enableV2MemoryPath(memoryBackend, memoryTarget);
            Path limit = memoryTarget.resolve("memory.max");
            if (!Files.isRegularFile(limit)) {
                throw new ResourceControlException("MEMORY_CONTROLLER_UNAVAILABLE");
            }
            if (targetMode == MemoryTargetMode.MANAGED
                    && !memoryTarget.equals(cpuTarget)
                    && desiredLimit > 0) {
                moveProcesses(cpuTarget.resolve("cgroup.procs"),
                        memoryTarget.resolve("cgroup.procs"));
            }
            String desired = desiredLimit == 0 ? "max" : String.valueOf(desiredLimit);
            if (!desired.equals(read(limit).trim())) {
                validateMemoryLimitAgainstUsage(
                        memoryTarget.resolve("memory.current"), desiredLimit);
                write(limit, desired);
            }
            String actual = read(limit).trim();
            if (!desired.equals(actual)) {
                throw new ResourceControlException("MEMORY_LIMIT_MISMATCH");
            }
            if (targetMode == MemoryTargetMode.MANAGED
                    && !memoryTarget.equals(cpuTarget)
                    && desiredLimit == 0) {
                moveProcesses(memoryTarget.resolve("cgroup.procs"),
                        memoryTarget.getParent().resolve("cgroup.procs"));
            }
            return new MemoryResult(memoryBackend.name,
                    "max".equals(actual) ? 0L : parseMemoryLimit(actual));
        }

        Path rootLimit = memoryBackend.root.resolve("memory.limit_in_bytes");
        if (!Files.isRegularFile(rootLimit)) {
            throw new ResourceControlException("MEMORY_CONTROLLER_UNAVAILABLE");
        }
        if (!Files.isDirectory(memoryTarget)) {
            if (targetMode == MemoryTargetMode.EXISTING
                    || desiredLimit == 0) {
                throw new ResourceControlException("MEMORY_CONTROLLER_UNAVAILABLE");
            }
            mkdir(memoryTarget);
        }
        Path limit = memoryTarget.resolve("memory.limit_in_bytes");
        if (!Files.isRegularFile(limit)) {
            throw new ResourceControlException("MEMORY_CONTROLLER_UNAVAILABLE");
        }
        if (targetMode == MemoryTargetMode.MANAGED
                && desiredLimit > 0) {
            moveProcesses(cpuTarget.resolve("cgroup.procs"),
                    memoryTarget.resolve("cgroup.procs"));
        }
        String desired = desiredLimit == 0
                ? read(rootLimit).trim()
                : String.valueOf(desiredLimit);
        if (!desired.equals(read(limit).trim())) {
            validateMemoryLimitAgainstUsage(
                    memoryTarget.resolve("memory.usage_in_bytes"), desiredLimit);
            write(limit, desired);
        }
        if (!desired.equals(read(limit).trim())) {
            throw new ResourceControlException("MEMORY_LIMIT_MISMATCH");
        }
        if (targetMode == MemoryTargetMode.MANAGED
                && desiredLimit == 0) {
            moveProcesses(memoryTarget.resolve("cgroup.procs"),
                    memoryTarget.getParent().resolve("cgroup.procs"));
        }
        return new MemoryResult(memoryBackend.name,
                desiredLimit == 0 ? 0L : parseMemoryLimit(read(limit).trim()));
    }

    private void enableV2MemoryPath(
            MemoryBackend backend, Path target) {
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
                        || !read(controllers).matches("(?s).*\\bmemory\\b.*")
                        || !Files.isRegularFile(control)) {
                    throw new ResourceControlException("MEMORY_CONTROLLER_UNAVAILABLE");
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
                "MEMORY_CONTROLLER_UNAVAILABLE",
                "MEMORY_LIMIT_MISMATCH");
    }

    private void moveProcessesToParent(Path target) {
        moveProcesses(
                target.resolve("cgroup.procs"),
                target.getParent().resolve("cgroup.procs"),
                "CPUSET_CONTROLLER_NOT_DELEGATED",
                "CPUSET_RELEASE_MISMATCH");
    }

    private void moveProcesses(
            Path source,
            Path destination,
            String unavailableReason,
            String mismatchReason) {
        if (!Files.isRegularFile(source) || !Files.isRegularFile(destination)) {
            throw new ResourceControlException(unavailableReason);
        }

        for (int attempt = 0; attempt < PROCESS_MOVE_ATTEMPTS; attempt++) {
            Set<String> destinationPids = processIds(destination);
            for (String pid : processIds(source)) {
                if (destinationPids.contains(pid)
                        || !Files.isDirectory(Paths.get("/proc", pid))) {
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
                if (Files.isDirectory(Paths.get("/proc", pid))
                        && !destinationPids.contains(pid)) {
                    complete = false;
                    break;
                }
            }
            if (complete) {
                return;
            }
        }
        throw new ResourceControlException(mismatchReason);
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
        return relative.getNameCount() > 0
                && relative.getName(0).toString().startsWith("zstack-role-");
    }

    private long parseMemoryLimit(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            throw new ResourceControlException("MEMORY_LIMIT_INVALID");
        }
    }

    private void validateMemoryLimitAgainstUsage(Path usage, long desiredLimit) {
        if (desiredLimit == 0) {
            return;
        }
        long current = Math.max(
                parseMemoryLimit(read(usage).trim()),
                residentMemoryUsage(usage.getParent().resolve("cgroup.procs")));
        if (desiredLimit < current) {
            throw new ResourceControlException("MEMORY_LIMIT_BELOW_CURRENT_USAGE");
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
                    throw new ResourceControlException("MEMORY_USAGE_INVALID");
                }
                try {
                    total = Math.addExact(
                            total, Math.multiplyExact(Long.parseLong(fields[1]), 1024L));
                } catch (NumberFormatException exception) {
                    throw new ResourceControlException("MEMORY_USAGE_INVALID");
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
            throw new ResourceControlException("MEMORY_LIMIT_INVALID");
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
                    throw new ResourceControlException("CGROUP_V2_SUBTREE_CONTROL_MISSING");
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
            throw new ResourceControlException("CPUSET_MEMS_EMPTY");
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
            throw new ResourceControlException("PARENT_CPUSET_EMPTY");
        }
        return value;
    }

    private Map<String, String> systemdProperties(String unit) {
        String output = run(null,
                "systemctl", "show", unit,
                "--property=LoadState", "--property=ActiveState",
                "--property=ControlGroup", "--property=MainPID");
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
            throw new ResourceControlException("READ_FAILED:" + path + ":" + exception.getMessage());
        }
    }

    private void write(Path path, String value) {
        run(value.getBytes(StandardCharsets.US_ASCII),
                "sudo", "-n", "tee", path.toString());
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
                throw new ResourceControlException("COMMAND_TIMEOUT:" + command[0]);
            }
            String output = readStream(process.getInputStream());
            if (process.exitValue() != 0) {
                throw new ResourceControlException(
                        "COMMAND_FAILED:" + command[0] + ":" + output.trim());
            }
            return output;
        } catch (IOException exception) {
            throw new ResourceControlException("COMMAND_FAILED:" + command[0] + ":" + exception.getMessage());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResourceControlException("COMMAND_INTERRUPTED:" + command[0]);
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
            throw new ResourceControlException("CONTROL_GROUP_PATH_INVALID");
        }
        return path;
    }

    private String stripRoot(String path) {
        return path.startsWith("/") ? path.substring(1) : path;
    }

    private String decodeMountPath(String value) {
        return value.replace("\\040", " ")
                .replace("\\011", "\t")
                .replace("\\012", "\n")
                .replace("\\134", "\\");
    }

    private String safeRole(String roleType) {
        String value = roleType == null ? "" : roleType.replaceAll("[^a-zA-Z0-9_.-]", "-");
        if (value.isEmpty()) {
            throw new ResourceControlException("ROLE_TYPE_INVALID");
        }
        return value;
    }

    private String errorType(ResourceControlException exception) {
        String message = exception.getMessage();
        if (message == null || message.isEmpty()) {
            return "RESOURCE_CONTROL_ERROR";
        }
        int separator = message.indexOf(':');
        return separator > 0 ? message.substring(0, separator) : message;
    }

    private static class Backend {
        private final String name;
        private final Path root;
        private final CgroupVersion version;

        private Backend(
                String name,
                Path root,
                CgroupVersion version) {
            this.name = name;
            this.root = root;
            this.version = version;
        }
    }

    private static class MemoryBackend {
        private final String name;
        private final Path root;
        private final CgroupVersion version;

        private MemoryBackend(
                String name,
                Path root,
                CgroupVersion version) {
            this.name = name;
            this.root = root;
            this.version = version;
        }
    }

    private enum CgroupVersion {
        V1,
        V2
    }

    private enum MemoryTargetMode {
        EXISTING,
        MANAGED
    }

    private enum ControlOperation {
        APPLY("READY"),
        RELEASE("DISABLED");

        private final String resultState;

        ControlOperation(String resultState) {
            this.resultState = resultState;
        }

        private static ControlOperation from(ResourceControlCommand command) {
            try {
                return valueOf(command.getOperation());
            } catch (IllegalArgumentException | NullPointerException error) {
                throw new ResourceControlException(
                        "RESOURCE_CONTROL_OPERATION_INVALID");
            }
        }
    }

    private static class MemoryResult {
        private final String backend;
        private final long actualLimit;

        private MemoryResult(String backend, long actualLimit) {
            this.backend = backend;
            this.actualLimit = actualLimit;
        }
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
                List<Path> v1CpuacctRoots,
                Path procMounts,
                Path systemdUnitRoot,
                CommandExecutor commandExecutor) {
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
                    Arrays.asList(
                            Paths.get("/sys/fs/cgroup/cpu,cpuacct"),
                            Paths.get("/sys/fs/cgroup/cpuacct")),
                    Paths.get("/proc/mounts"),
                    Paths.get("/etc/systemd/system"),
                    null);
        }
    }

    public static class ResourceControlException extends RuntimeException {
        public ResourceControlException(String message) {
            super(message);
        }
    }
}
