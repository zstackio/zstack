package org.zstack.portal.managementnode;

import org.zstack.header.physicalserver.PhysicalServerCpuSet;
import org.zstack.portal.managementnode.ResourceControlUtils.CommandExecutor;
import org.zstack.portal.managementnode.ResourceControlUtils.ResourceControlException;
import org.zstack.utils.data.SizeUnit;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.zstack.portal.managementnode.ResourceControlUtils.read;
import static org.zstack.portal.managementnode.ResourceControlUtils.stripRoot;
import static org.zstack.portal.managementnode.ResourceControlUtils.underRoot;

abstract class CgroupResourceControlBackend {
    private static final int PROCESS_MOVE_ATTEMPTS = 3;
    static final long NANOSECONDS_PER_MICROSECOND = 1000;
    private static final long BYTES_PER_KIBIBYTE = SizeUnit.KILOBYTE.toByte(1);
    private static final String DIRECTORY_MODE = "0755";
    final ResourceControlUtils commands;
    final Path root;

    CgroupResourceControlBackend(Path root, CommandExecutor commandExecutor) {
        commands = new ResourceControlUtils(commandExecutor);
        this.root = root;
    }

    abstract boolean supportsSubtreeMemoryLimit(Path target);
    abstract boolean supportsExclusiveCpuPartition();
    abstract String cpuSetProperty();
    abstract String memoryLimitProperty();
    abstract String memoryUsageFile();
    abstract boolean matchesCpuHierarchy(String[] fields);
    abstract void enableCpuController(Path target);
    abstract void initializeCpus(Path target);
    abstract void requireCpuTarget(Path target);
    abstract void releaseCpuSet(Path target);
    abstract String applyExclusiveCpuSet(Path target, String desired);
    abstract long applyMemory(Path target, long desired, Path cpuTarget);
    abstract Long readMemoryLimit(Path target);
    abstract Long readCpuTime(Path relative);

    void prepareCpuGroup(Path target) {
        enableCpuController(target);
        initializeMems(target);
        initializeCpus(target);
    }

    void attachProcesses(Path source, Path target) {
        mkdir(target);
        prepareCpuGroup(target);
        moveProcesses(source.resolve("cgroup.procs"), target.resolve("cgroup.procs"),
                "Systemd control group process files are unavailable",
                "Some systemd unit processes could not be moved");
    }

    void requireSubtreeMemoryLimit(Path target) {
        if (!supportsSubtreeMemoryLimit(target)) {
            throw new ResourceControlException(String.format(
                    "Subtree memory limits are not supported for control group[%s]", target));
        }
    }

    void validateMemoryLimit(Path target, long desired) {
        validateMemoryLimitAgainstUsage(target.resolve(memoryUsageFile()), desired);
    }

    Long readMemoryUsage(Path target) {
        Path usage = target.resolve(memoryUsageFile());
        return Files.isRegularFile(usage) ? parseMemoryLimit(read(usage).trim()) : null;
    }

    void resetCpuSet(Path target, String desired) {
        Path cpuFile = target.resolve("cpuset.cpus");
        if (!Files.isRegularFile(cpuFile)) {
            throw new ResourceControlException(String.format(
                    "Cpuset controller is not available for control group[%s]", target));
        }
        if (!normalizeOptional(read(cpuFile)).equals(desired)) {
            commands.write(cpuFile, desired.isEmpty() ? "\n" : desired);
        }
        if (!normalizeOptional(read(cpuFile)).equals(desired)) {
            throw new ResourceControlException(String.format(
                    "Failed to release CPU set from control group[%s]", target));
        }
    }

    String applyCpuSet(Path target, String desired) {
        enableCpuController(target);
        initializeMems(target);
        Path cpuFile = target.resolve("cpuset.cpus");
        if (!Files.isRegularFile(cpuFile)) {
            throw new ResourceControlException(String.format(
                    "Cpuset controller is not available for control group[%s]", target));
        }
        String configured = normalizeOptional(read(cpuFile));
        if (!configured.equals(desired)) {
            commands.write(cpuFile, desired);
        }
        Path effective = target.resolve("cpuset.cpus.effective");
        return normalizeOptional(read(Files.isRegularFile(effective) ? effective : cpuFile));
    }

    String readCpuSet(Path target) {
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
            if (current.equals(root)) {
                throw new ResourceControlException(String.format(
                        "Control group[%s] and its parents have no effective CPU set", target));
            }
            current = underRoot(root, current.getParent());
        }
    }

    Path processGroup(String pid) {
        for (String line : read(Paths.get("/proc", pid, "cgroup")).split("\\R")) {
            String[] fields = line.split(":", 3);
            if (fields.length != 3) {
                continue;
            }
            if (!matchesCpuHierarchy(fields)) {
                continue;
            }
            Path target = underRoot(root, root.resolve(stripRoot(fields[2])).normalize());
            if (Files.isDirectory(target)) {
                return target;
            }
        }
        throw new ResourceControlException(String.format("No cpuset control group was found for process[%s]", pid));
    }

    void moveProcesses(Path source, Path destination, String unavailableMessage, String mismatchMessage) {
        if (!Files.isRegularFile(source) || !Files.isRegularFile(destination)) {
            throw new ResourceControlException(unavailableMessage);
        }

        for (int attempt = 0; attempt < PROCESS_MOVE_ATTEMPTS; attempt++) {
            Set<String> destinationPids = processIds(destination);
            for (String pid : processIds(source)) {
                if (destinationPids.contains(pid) || !Files.isDirectory(Paths.get("/proc", pid))) {
                    continue;
                }
                commands.write(destination, pid);
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

    void moveProcesses(Path source, Path destination) {
        moveProcesses(source, destination,
                "Memory controller process files are unavailable",
                "Some processes could not be moved to the memory control group");
    }

    void moveProcessesToParent(Path target) {
        moveProcesses(target.resolve("cgroup.procs"), target.getParent().resolve("cgroup.procs"),
                "Cpuset controller process files are unavailable",
                "Some processes could not be moved to the parent control group");
    }

    Set<String> processIds(Path source) {
        Set<String> pids = new LinkedHashSet<>();
        for (String pid : read(source).trim().split("\\s+")) {
            if (pid.matches("[1-9][0-9]*")) {
                pids.add(pid);
            }
        }
        return pids;
    }

    boolean managedGroup(Path root, Path target) {
        Path relative = root.relativize(target);
        return relative.getNameCount() > 0 && relative.getName(0).toString().startsWith("zstack-role-");
    }

    long parseMemoryLimit(String value) {
        if (value == null || !value.matches("[0-9]+")) {
            throw new ResourceControlException(String.format("Memory value[%s] is not a valid byte count", value));
        }
        return Long.parseLong(value);
    }

    void validateMemoryLimitAgainstUsage(Path usage, long desiredLimit) {
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

    long residentMemoryUsage(Path processFile) {
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
            String value = read(status);
            for (String line : value.split("\\R")) {
                if (!line.startsWith("VmRSS:")) {
                    continue;
                }
                String[] fields = line.trim().split("\\s+");
                if (fields.length < 2 || !fields[1].matches("[0-9]+")) {
                    throw new ResourceControlException(String.format(
                            "Process[%s] reported invalid resident memory usage[%s]", pid, line));
                }
                long kibibytes = Long.parseLong(fields[1]);
                if (kibibytes > Long.MAX_VALUE / BYTES_PER_KIBIBYTE
                        || total > Long.MAX_VALUE - kibibytes * BYTES_PER_KIBIBYTE) {
                    return Long.MAX_VALUE;
                }
                total += kibibytes * BYTES_PER_KIBIBYTE;
                break;
            }
        }
        return total;
    }

    void initializeMems(Path target) {
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
        commands.write(mems, value);
    }

    String parentCpuSet(Path target) {
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

    void mkdir(Path path) {
        if (!Files.isDirectory(path)) {
            commands.run(null, "sudo", "-n", "mkdir", "-p", path.toString());
        }
        commands.run(null, "sudo", "-n", "chmod", DIRECTORY_MODE, path.toString());
    }

    static String normalizeOptional(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? "" : PhysicalServerCpuSet.normalize(trimmed);
    }

    Path target(Path relative) {
        return underRoot(root, root.resolve(relative).normalize());
    }

    Path controlGroupPath(String controlGroup) {
        return underRoot(root, root.resolve(stripRoot(controlGroup)).normalize());
    }

    Path findTarget(String controlGroup) {
        if (controlGroup == null || controlGroup.isEmpty()) {
            return null;
        }
        Path target = controlGroupPath(controlGroup);
        return target.equals(root) ? null : existingTarget(target);
    }

    Path existingTarget(Path target) {
        return Files.isDirectory(target) ? target : null;
    }

    boolean contains(Path target, String controlGroup) {
        Path current = findTarget(controlGroup);
        return current != null && current.startsWith(target);
    }

    boolean hasProcessFile(Path target) {
        return Files.isRegularFile(target.resolve("cgroup.procs"));
    }

    boolean hasProcesses(Path target) {
        return Files.isDirectory(target) && hasProcessFile(target)
                && !processIds(target.resolve("cgroup.procs")).isEmpty();
    }
}
