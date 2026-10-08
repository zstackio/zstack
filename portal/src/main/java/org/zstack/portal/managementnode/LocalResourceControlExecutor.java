package org.zstack.portal.managementnode;

import org.zstack.core.CoreGlobalProperty;
import org.zstack.header.physicalserver.ManagedServiceResourceUsage;
import org.zstack.header.physicalserver.ManagedServiceResourceUsage.State;
import org.zstack.header.physicalserver.PhysicalServerResourceIsolationMode;
import org.zstack.header.physicalserver.ResourceConsumerHandle;
import org.zstack.header.physicalserver.ResourceControlCommand;
import org.zstack.portal.managementnode.ResourceControlUtils.CommandExecutor;
import org.zstack.portal.managementnode.ResourceControlUtils.ResourceControlException;
import org.zstack.utils.data.SizeUnit;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.stream.Collectors.toSet;
import static org.zstack.portal.managementnode.CgroupResourceControlBackend.normalizeOptional;
import static org.zstack.portal.managementnode.ResourceControlUtils.stripRoot;

public class LocalResourceControlExecutor {
    private final AtomicInteger testCalls = new AtomicInteger();
    private volatile boolean testMode;
    private volatile ResourceControlCommand lastTestCommand;
    private final AtomicReference<List<ResourceConsumerHandle>> lastTestRestartHandles = new AtomicReference<>();
    private final SystemdResourceControlUtils systemd;
    private final CgroupResourceControlBackendFactory backendFactory;

    public LocalResourceControlExecutor() {
        this(ExecutionEnvironment.system());
        testMode = CoreGlobalProperty.UNIT_TEST_ON;
    }

    LocalResourceControlExecutor(ExecutionEnvironment environment) {
        systemd = new SystemdResourceControlUtils(
                environment.systemdUnitRoot, environment.v1SystemdRoot, environment.commandExecutor);
        backendFactory = new CgroupResourceControlBackendFactory(
                environment.v2Root, environment.v1Root, environment.v1MemoryRoot,
                environment.v1CpuacctRoots, environment.procMounts, environment.commandExecutor);
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
        CgroupResourceControlBackend backend = backendFactory.cpu();
        if (isolationMode == PhysicalServerResourceIsolationMode.EXCLUSIVE
                && !backend.supportsExclusiveCpuPartition()) {
            throw new ResourceControlException("Exclusive CPU partitions are not supported by the cgroup backend");
        }
        Long desiredMemory = command.getMemory();
        CgroupResourceControlBackend memoryBackend = null;
        if (desiredMemory != null) {
            memoryBackend = backendFactory.memory();
        }
        if (command.getSliceName() != null
                && command.getHandles().stream().anyMatch(handle ->
                ResourceConsumerHandle.SYSTEMD_UNIT.equals(handle.getHandleType()))) {
            return applySystemdSlice(command, backend, memoryBackend, desired, desiredMemory);
        }
        List<Boolean> results = new ArrayList<>();
        for (ResourceConsumerHandle handle : command.getHandles()) {
            results.add(applyNonSystemdHandle(command, backend, memoryBackend, handle, desired, desiredMemory));
        }
        return summarizeApply(results);
    }

    public boolean release(ResourceControlCommand command) {
        if (CoreGlobalProperty.UNIT_TEST_ON && testMode) {
            return fakeRelease(command);
        }

        CgroupResourceControlBackend backend = backendFactory.cpu();
        CgroupResourceControlBackend memoryBackend = backendFactory.findMemory();
        if (command.getSliceName() != null
                && command.getHandles().stream().anyMatch(handle ->
                ResourceConsumerHandle.SYSTEMD_UNIT.equals(handle.getHandleType()))) {
            return releaseSystemdSlice(command, backend, memoryBackend);
        }
        List<Boolean> results = new ArrayList<>();
        for (ResourceConsumerHandle handle : command.getHandles()) {
            results.add(releaseNonSystemdHandle(command, backend, memoryBackend, handle));
        }
        return summarizeRelease(results);
    }

    private boolean applySystemdSlice(ResourceControlCommand command,
            CgroupResourceControlBackend backend, CgroupResourceControlBackend memoryBackend,
            String desired, Long desiredMemory) {
        boolean manageCpu = !desired.isEmpty();
        boolean memoryError = desiredMemory != null && desiredMemory > 0
                && !validateActiveSliceMemory(memoryBackend, command.getSliceName(), desiredMemory);
        boolean changed = systemd.pruneServices(command.getSliceName(), command.getHandles().stream()
                .filter(handle -> ResourceConsumerHandle.SYSTEMD_UNIT.equals(handle.getHandleType()))
                .map(ResourceConsumerHandle::getValue).collect(toSet()));
        changed = systemd.configureSlice(command.getSliceName(), backend.cpuSetProperty(), desired,
                memoryBackend == null ? null : memoryBackend.memoryLimitProperty(), desiredMemory) || changed;
        for (ResourceConsumerHandle handle : command.getHandles()) {
            if (ResourceConsumerHandle.SYSTEMD_UNIT.equals(handle.getHandleType())) {
                changed = systemd.configureService(handle.getValue(), command.getSliceName()) || changed;
            }
        }
        boolean legacyCpuFallback = false;
        Map<Integer, Boolean> legacyCpuResults = new HashMap<>();
        Path sliceTarget = ensureActiveSliceTarget(backend, command.getSliceName());
        if (sliceTarget == null) {
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
                    if (!command.getSliceName().equals(systemd.configuredSlice(handle.getValue()))) {
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
            actualCpuSet = isolationMode(command) == PhysicalServerResourceIsolationMode.EXCLUSIVE
                    ? backend.applyExclusiveCpuSet(sliceTarget, desired) : backend.applyCpuSet(sliceTarget, desired);
        }
        if (desiredMemory != null && memoryBackend != null) {
            memorySliceTarget = activeSliceTarget(memoryBackend, command.getSliceName());
            if (memorySliceTarget != null) {
                if (desiredMemory > 0) {
                    memoryBackend.requireSubtreeMemoryLimit(memorySliceTarget);
                }
                actualMemory = memoryBackend.applyMemory(memorySliceTarget, desiredMemory, null);
            }
        }
        if (changed) {
            systemd.reload();
        }
        if (memoryError) {
            throw new ResourceControlException(String.format(
                    "Memory control group for active systemd slice[%s] does not exist", command.getSliceName()));
        }

        List<Boolean> results = new ArrayList<>();
        for (int index = 0; index < command.getHandles().size(); index++) {
            ResourceConsumerHandle handle = command.getHandles().get(index);
            if (!ResourceConsumerHandle.SYSTEMD_UNIT.equals(handle.getHandleType())) {
                results.add(applyNonSystemdHandle(command, backend, memoryBackend, handle, desired, desiredMemory));
                continue;
            }
            if (!command.getSliceName().equals(systemd.configuredSlice(handle.getValue()))) {
                results.add(null);
                continue;
            }
            Map<String, String> properties = systemd.properties(handle.getValue());
            if ("not-found".equals(properties.get("LoadState"))) {
                if (handle.isOptional()) {
                    results.add(null);
                    continue;
                }
                throw new ResourceControlException(String.format("Systemd unit[%s] does not exist", handle.getValue()));
            }
            if (!"active".equals(properties.get("ActiveState"))) {
                results.add(!manageCpu || backend.cpuSetProperty() != null ? Boolean.TRUE : null);
                continue;
            }
            boolean cpuSynced;
            if (legacyCpuFallback) {
                if (!legacyCpuResults.containsKey(index)) {
                    throw new ResourceControlException(String.format(
                            "No CPU control result was found for systemd unit[%s]", handle.getValue()));
                }
                Boolean cpuResult = legacyCpuResults.get(index);
                if (cpuResult == null) {
                    results.add(null);
                    continue;
                }
                cpuSynced = cpuResult;
            } else {
                Path current = backend.findTarget(properties.get("ControlGroup"));
                if (current == null) {
                    throw new ResourceControlException(String.format(
                            "No control group was found for systemd unit[%s]", handle.getValue()));
                }
                if (sliceTarget == null || !current.startsWith(sliceTarget)) {
                    results.add(false);
                    continue;
                }
                cpuSynced = !manageCpu || desired.equals(normalizeOptional(actualCpuSet));
            }
            if (command.getMemory() != null && memorySliceTarget != null
                    && !memoryBackend.contains(memorySliceTarget, properties.get("ControlGroup"))) {
                results.add(false);
                continue;
            }
            results.add(cpuSynced && memoryMatches(actualMemory, desiredMemory));
        }
        return summarizeApply(results);
    }

    private boolean releaseSystemdSlice(ResourceControlCommand command,
            CgroupResourceControlBackend backend, CgroupResourceControlBackend memoryBackend) {
        boolean changed = systemd.releaseSlice(command.getSliceName());
        if (changed) {
            systemd.reload();
        }
        if (memoryBackend != null) {
            Path memoryTarget = activeSliceTarget(memoryBackend, command.getSliceName());
            if (memoryTarget != null) {
                memoryBackend.applyMemory(memoryTarget, 0L, null);
            }
        }
        Path sliceTarget = activeSliceTarget(backend, command.getSliceName());
        if (sliceTarget == null) {
            List<Boolean> results = new ArrayList<>();
            for (ResourceConsumerHandle handle : command.getHandles()) {
                results.add(releaseNonSystemdHandle(command, backend, memoryBackend, handle));
            }
            return summarizeRelease(results);
        }
        backend.releaseCpuSet(sliceTarget);
        return true;
    }

    private boolean validateActiveSliceMemory(
            CgroupResourceControlBackend memoryBackend, String sliceName, long desiredMemory) {
        Map<String, String> properties = systemd.properties(sliceName);
        if (!"active".equals(properties.get("ActiveState"))) {
            return true;
        }
        Path memoryTarget = memoryBackend.findTarget(properties.get("ControlGroup"));
        if (memoryTarget == null) {
            return false;
        }
        memoryBackend.requireSubtreeMemoryLimit(memoryTarget);
        memoryBackend.validateMemoryLimit(memoryTarget, desiredMemory);
        return true;
    }

    private Boolean applyNonSystemdHandle(ResourceControlCommand command,
            CgroupResourceControlBackend backend, CgroupResourceControlBackend memoryBackend,
            ResourceConsumerHandle handle, String desired, Long desiredMemory) {
        Path target = resolve(backend, command.getRoleType(), handle);
        if (target == null) {
            return null;
        }
        String actualCpuSet = null;
        if (!desired.isEmpty()) {
            actualCpuSet = isolationMode(command) == PhysicalServerResourceIsolationMode.EXCLUSIVE
                    ? backend.applyExclusiveCpuSet(target, desired) : backend.applyCpuSet(target, desired);
        }
        Long actualMemory = null;
        if (desiredMemory != null) {
            actualMemory = applyMemoryLimit(backend, memoryBackend, target, desiredMemory);
        }
        return (desired.isEmpty() || desired.equals(normalizeOptional(actualCpuSet)))
                && memoryMatches(actualMemory, desiredMemory);
    }

    private Boolean releaseNonSystemdHandle(ResourceControlCommand command,
            CgroupResourceControlBackend backend, CgroupResourceControlBackend memoryBackend,
            ResourceConsumerHandle handle) {
        Path target = resolveForRelease(backend, command.getRoleType(), handle);
        if (target == null) {
            return null;
        }
        backend.releaseCpuSet(target);
        Long actualMemory = 0L;
        if (memoryBackend != null) {
            actualMemory = applyMemoryLimit(backend, memoryBackend, target, 0L);
        }
        return memoryMatches(actualMemory, 0L);
    }

    private Path ensureActiveSliceTarget(CgroupResourceControlBackend backend, String sliceName) {
        Map<String, String> properties = systemd.properties(sliceName);
        if (!"active".equals(properties.get("ActiveState"))) {
            systemd.start(sliceName);
        }
        return activeSliceTarget(backend, sliceName);
    }

    private Path activeSliceTarget(CgroupResourceControlBackend backend, String sliceName) {
        Map<String, String> properties = systemd.properties(sliceName);
        if (!"active".equals(properties.get("ActiveState"))) {
            return null;
        }
        return backend.findTarget(properties.get("ControlGroup"));
    }

    public List<ManagedServiceResourceUsage> inspect(String roleType, List<ResourceConsumerHandle> handles) {
        return inspect(roleType, null, handles);
    }

    public List<ManagedServiceResourceUsage> inspect(
            String roleType, String sliceName, List<ResourceConsumerHandle> handles) {
        if (CoreGlobalProperty.UNIT_TEST_ON && testMode) {
            return fakeInspect(handles);
        }
        CgroupResourceControlBackend backend = backendFactory.cpu();
        List<ManagedServiceResourceUsage> result = new ArrayList<>();
        Map<String, Path> sliceTargets = new HashMap<>();
        for (ResourceConsumerHandle handle : handles) {
            String configuredSlice = systemd.configuredSlice(handle.getValue());
            if (sliceName != null && configuredSlice != null && !sliceName.equals(configuredSlice)) {
                continue;
            }
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
            CgroupResourceControlBackend backend,
            String roleType,
            ResourceConsumerHandle handle, State state, Path current, Map<String, Path> sliceTargets) {
        if (state != State.RUNNING || !ResourceConsumerHandle.SYSTEMD_UNIT.equals(handle.getHandleType())) {
            return false;
        }
        String sliceName = systemd.configuredSlice(handle.getValue());
        if (sliceName == null) {
            return false;
        }
        Path managed = backend.root.resolve(String.format(
                "zstack-role-%s-unit-%s", safeRole(roleType), safeRole(handle.getValue())));
        if (current.equals(managed) || backend.hasProcesses(managed)) {
            return false;
        }
        Path sliceTarget = sliceTarget(backend, sliceName, sliceTargets);
        return sliceTarget == null || !current.startsWith(sliceTarget);
    }

    private Path sliceTarget(CgroupResourceControlBackend backend, String sliceName, Map<String, Path> sliceTargets) {
        if (sliceTargets.containsKey(sliceName)) {
            return sliceTargets.get(sliceName);
        }
        Map<String, String> properties = systemd.properties(sliceName);
        Path target = "active".equals(properties.get("ActiveState"))
                ? backend.findTarget(properties.get("ControlGroup")) : null;
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

        for (ResourceConsumerHandle handle : handles) {
            Map<String, String> properties = systemd.properties(handle.getValue());
            if ("not-found".equals(properties.get("LoadState"))) {
                throw new ResourceControlException(String.format("Systemd unit[%s] does not exist", handle.getValue()));
            }
            if (!"active".equals(properties.get("ActiveState"))) {
                throw new ResourceControlException(String.format("Systemd unit[%s] is not active", handle.getValue()));
            }
            if (!sliceName.equals(systemd.configuredSlice(handle.getValue()))) {
                throw new ResourceControlException(String.format(
                        "Systemd unit[%s] is not configured for slice[%s]", handle.getValue(), sliceName));
            }
        }

        CgroupResourceControlBackend backend = backendFactory.cpu();
        Path sliceTarget = activeSliceTarget(backend, sliceName);
        backend.requireCpuTarget(sliceTarget);

        for (ResourceConsumerHandle handle : handles) {
            systemd.restart(handle.getValue());
            Map<String, String> properties = systemd.properties(handle.getValue());
            if (!"active".equals(properties.get("ActiveState"))) {
                throw new ResourceControlException(String.format(
                        "Systemd unit[%s] is not active after restart", handle.getValue()));
            }
            if (sliceTarget != null
                    && !backend.contains(sliceTarget, properties.get("ControlGroup"))) {
                throw new ResourceControlException(String.format(
                        "Systemd unit[%s] did not enter slice[%s] after restart", handle.getValue(), sliceName));
            }
        }
    }

    private List<ManagedServiceResourceUsage> fakeInspect(List<ResourceConsumerHandle> handles) {
        List<ManagedServiceResourceUsage> result = new ArrayList<>();
        for (ResourceConsumerHandle handle : handles) {
            ManagedServiceResourceUsage usage = serviceUsage(handle, State.RUNNING);
            if (lastTestCommand != null) {
                usage.setCpuSet(lastTestCommand.getCpuSet());
                usage.setMemoryLimit(lastTestCommand.getMemory());
            }
            result.add(usage);
        }
        return result;
    }

    private ManagedServiceResourceUsage serviceUsage(ResourceConsumerHandle handle, State state) {
        ManagedServiceResourceUsage usage = new ManagedServiceResourceUsage();
        usage.setServiceName(handle.getServiceName());
        usage.setRestartable(handle.isRestartable());
        usage.setRestartRequired(false);
        usage.setState(state.name());
        return usage;
    }

    private ServiceTarget inspectTarget(
            CgroupResourceControlBackend backend, String roleType, ResourceConsumerHandle handle) {
        if (ResourceConsumerHandle.SYSTEMD_UNIT.equals(handle.getHandleType())) {
            Map<String, String> properties = systemd.properties(handle.getValue());
            if ("not-found".equals(properties.get("LoadState"))) {
                return new ServiceTarget(State.NOT_FOUND, null);
            }
            if (!"active".equals(properties.get("ActiveState"))) {
                return new ServiceTarget(State.INACTIVE, null);
            }
            String controlGroup = properties.get("ControlGroup");
            if (controlGroup != null && !controlGroup.isEmpty()) {
                Path current = backend.controlGroupPath(controlGroup);
                if (backend.existingTarget(current) != null) {
                    return new ServiceTarget(State.RUNNING, current);
                }
            }
            Path managed = backend.root.resolve(String.format(
                    "zstack-role-%s-unit-%s", safeRole(roleType), safeRole(handle.getValue())));
            if (backend.hasProcesses(managed)) {
                return new ServiceTarget(State.RUNNING, managed);
            }
            String mainPid = properties.get("MainPID");
            if (mainPid != null && mainPid.matches("[1-9][0-9]*")) {
                return new ServiceTarget(State.RUNNING, backend.processGroup(mainPid));
            }
            throw new ResourceControlException(String.format(
                    "No control group was found for systemd unit[%s]", handle.getValue()));
        }
        throw new ResourceControlException(String.format(
                "Resource consumer handle type[%s] is unsupported", handle.getHandleType()));
    }

    private void fillUsage(ManagedServiceResourceUsage usage,
            CgroupResourceControlBackend backend, Path target, ResourceConsumerHandle handle) {
        usage.setCpuSet(backend.readCpuSet(target));
        Path relative = backend.root.relativize(target);
        String controlGroup = null;
        if (ResourceConsumerHandle.SYSTEMD_UNIT.equals(handle.getHandleType())) {
            controlGroup = systemd.properties(handle.getValue()).get("ControlGroup");
        }
        usage.setCpuTime(cpuTime(relative, controlGroup));
        CgroupResourceControlBackend memoryBackend = backendFactory.findMemory();
        if (memoryBackend == null) {
            return;
        }
        Path memoryTarget = memoryBackend.target(relative);
        if (controlGroup != null && !controlGroup.isEmpty()) {
            Path current = memoryBackend.target(Paths.get(stripRoot(controlGroup)));
            if (memoryBackend.existingTarget(current) != null) {
                memoryTarget = current;
            }
        }
        if (memoryBackend.existingTarget(memoryTarget) == null) {
            return;
        }
        usage.setMemory(memoryBackend.readMemoryUsage(memoryTarget));
        usage.setMemoryLimit(memoryBackend.readMemoryLimit(memoryTarget));
    }

    private Long cpuTime(Path relative, String controlGroup) {
        if (!relative.toString().isEmpty()) {
            for (CgroupResourceControlBackend backend : backendFactory.cpuAccounting()) {
                Long value = backend.readCpuTime(relative);
                if (value != null) {
                    return value;
                }
            }
        }
        if (controlGroup == null || controlGroup.isEmpty()) {
            return null;
        }
        for (CgroupResourceControlBackend backend : backendFactory.legacyCpuAccounting()) {
            Long value = backend.readCpuTime(Paths.get(stripRoot(controlGroup)));
            if (value != null) {
                return value;
            }
        }
        return null;
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
        normalizeOptional(command.getCpuSet());
        validateMemoryLimit(command.getMemory());
        return !command.getHandles().isEmpty();
    }

    private boolean fakeRelease(ResourceControlCommand command) {
        lastTestCommand = command;
        testCalls.incrementAndGet();
        return true;
    }

    private boolean summarizeApply(List<Boolean> results) {
        return results.contains(Boolean.TRUE) && !results.contains(Boolean.FALSE);
    }

    private boolean summarizeRelease(List<Boolean> results) {
        return !results.contains(Boolean.FALSE);
    }

    private boolean memoryMatches(Long actual, Long desired) {
        return desired == null || desired.equals(actual) || desired == 0L && actual == null;
    }

    private Path resolve(CgroupResourceControlBackend backend, String roleType, ResourceConsumerHandle handle) {
        if (ResourceConsumerHandle.SYSTEMD_UNIT.equals(handle.getHandleType())) {
            return resolveSystemd(backend, roleType, handle);
        }
        throw new ResourceControlException(String.format(
                "Resource consumer handle type[%s] is unsupported", handle.getHandleType()));
    }

    private Path resolveSystemd(CgroupResourceControlBackend backend, String roleType, ResourceConsumerHandle handle) {
        Path managedTarget = backend.root.resolve(String.format(
                "zstack-role-%s-unit-%s", safeRole(roleType), safeRole(handle.getValue())));
        Map<String, String> properties = systemd.properties(handle.getValue());
        if ("not-found".equals(properties.get("LoadState"))) {
            if (handle.isOptional()) {
                return null;
            }
            throw new ResourceControlException(String.format("Systemd unit[%s] does not exist", handle.getValue()));
        }
        if (!"active".equals(properties.get("ActiveState"))) {
            return null;
        }
        String controlGroup = properties.get("ControlGroup");
        if (controlGroup == null || controlGroup.isEmpty()) {
            throw new ResourceControlException(String.format(
                    "Systemd unit[%s] did not report a control group", handle.getValue()));
        }
        Path target = backend.controlGroupPath(controlGroup);
        if (target.equals(backend.root)) {
            throw new ResourceControlException(String.format(
                    "Systemd unit[%s] reported the root control group", handle.getValue()));
        }
        if (backend.existingTarget(target) == null) {
            return resolveSystemdFallback(backend, handle, controlGroup, managedTarget);
        }
        return target;
    }

    private Path resolveForRelease(
            CgroupResourceControlBackend backend, String roleType, ResourceConsumerHandle handle) {
        if (!ResourceConsumerHandle.SYSTEMD_UNIT.equals(handle.getHandleType())) {
            throw new ResourceControlException(String.format(
                    "Resource consumer handle type[%s] is unsupported", handle.getHandleType()));
        }
        Path managedTarget = backend.root.resolve(String.format(
                "zstack-role-%s-unit-%s", safeRole(roleType), safeRole(handle.getValue())));
        if (backend.existingTarget(managedTarget) != null) {
            return managedTarget;
        }
        Map<String, String> properties = systemd.properties(handle.getValue());
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
            throw new ResourceControlException(String.format(
                    "Systemd unit[%s] did not report a control group", handle.getValue()));
        }
        Path target = backend.controlGroupPath(controlGroup);
        if (target.equals(backend.root)) {
            throw new ResourceControlException(String.format(
                    "Systemd unit[%s] reported the root control group", handle.getValue()));
        }
        return backend.existingTarget(target);
    }

    private Path resolveSystemdFallback(
            CgroupResourceControlBackend backend, ResourceConsumerHandle handle, String controlGroup, Path target) {
        Path source = systemd.legacyControlGroup(controlGroup);
        if (!backend.hasProcessFile(source)) {
            if (handle.isOptional()) {
                return null;
            }
            throw new ResourceControlException(String.format("Systemd control group[%s] does not exist", controlGroup));
        }
        if (!backend.hasProcesses(source)) {
            if (handle.isOptional()) {
                return null;
            }
            throw new ResourceControlException(String.format(
                    "Systemd control group[%s] contains no processes", controlGroup));
        }
        backend.attachProcesses(source, target);
        return target;
    }

    private PhysicalServerResourceIsolationMode isolationMode(ResourceControlCommand command) {
        return command.getIsolationMode() == null
                ? PhysicalServerResourceIsolationMode.SHARED : command.getIsolationMode();
    }

    private long applyMemoryLimit(CgroupResourceControlBackend backend, CgroupResourceControlBackend memoryBackend,
            Path cpuTarget, long desiredLimit) {
        if (memoryBackend == null) {
            throw new ResourceControlException("Memory controller is unavailable for the target control group");
        }
        Path relative = backend.root.relativize(cpuTarget);
        Path memoryTarget = memoryBackend.target(relative);
        return memoryBackend.applyMemory(memoryTarget, desiredLimit, cpuTarget);
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

    private String safeRole(String roleType) {
        String value = roleType == null ? "" : roleType.replaceAll("[^a-zA-Z0-9_.-]", "-");
        if (value.isEmpty()) {
            throw new ResourceControlException("Role type must contain at least one valid path character");
        }
        return value;
    }

    private static class ServiceTarget {
        private final State state;
        private final Path path;

        private ServiceTarget(State state, Path path) {
            this.state = state;
            this.path = path;
        }
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

}
