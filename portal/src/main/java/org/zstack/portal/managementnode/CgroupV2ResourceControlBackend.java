package org.zstack.portal.managementnode;

import org.zstack.portal.managementnode.ResourceControlUtils.CommandExecutor;
import org.zstack.portal.managementnode.ResourceControlUtils.ResourceControlException;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.zstack.portal.managementnode.ResourceControlUtils.read;
import static org.zstack.portal.managementnode.ResourceControlUtils.underRoot;

class CgroupV2ResourceControlBackend extends CgroupResourceControlBackend {
    CgroupV2ResourceControlBackend(Path root, CommandExecutor commandExecutor) {
        super(root, commandExecutor);
    }

    @Override
    boolean supportsSubtreeMemoryLimit(Path target) {
        return true;
    }

    @Override
    boolean supportsExclusiveCpuPartition() {
        return true;
    }

    @Override
    String cpuSetProperty() {
        return "AllowedCPUs";
    }

    @Override
    String memoryLimitProperty() {
        return "MemoryMax";
    }

    @Override
    String memoryUsageFile() {
        return "memory.current";
    }

    @Override
    boolean matchesCpuHierarchy(String[] fields) {
        return "0".equals(fields[0]);
    }

    @Override
    void initializeCpus(Path target) {
    }

    @Override
    void requireCpuTarget(Path target) {
        if (target == null) {
            throw new ResourceControlException("The target control group is not active in the cpuset hierarchy");
        }
    }

    @Override
    String applyCpuSet(Path target, String desired) {
        makePartitionMember(target);
        return super.applyCpuSet(target, desired);
    }

    @Override
    String applyExclusiveCpuSet(Path target, String desired) {
        enableCpuController(target);
        if (!Files.isRegularFile(target.resolve("cpuset.cpus.partition"))) {
            throw new ResourceControlException(String.format(
                    "CPU partition interface is unavailable for control group[%s]", target));
        }
        super.applyCpuSet(target, desired);
        makePartitionRoot(target, desired);
        Path effective = target.resolve("cpuset.cpus.effective");
        return normalizeOptional(read(Files.isRegularFile(effective) ? effective : target.resolve("cpuset.cpus")));
    }

    @Override
    void releaseCpuSet(Path target) {
        makePartitionMember(target);
        enableCpuController(target);
        initializeMems(target);
        String desired;
        if (managedGroup(root, target)) {
            moveProcessesToParent(target);
            desired = "";
        } else {
            desired = parentCpuSet(target);
        }
        resetCpuSet(target, desired);
    }

    @Override
    void enableCpuController(Path target) {
        Path current = root;
        for (Path part : root.relativize(target)) {
            Path child = current.resolve(part);
            if (!Files.isRegularFile(child.resolve("cpuset.cpus"))) {
                Path control = current.resolve("cgroup.subtree_control");
                if (!Files.isRegularFile(control)) {
                    throw new ResourceControlException(String.format(
                            "Cgroup v2 subtree control is unavailable for control group[%s]", current));
                }
                commands.write(control, "+cpuset");
            }
            current = child;
        }
    }

    void enableMemoryController(Path target) {
        if (Files.isRegularFile(target.resolve("memory.max"))) {
            return;
        }
        Path current = root;
        for (Path part : root.relativize(target)) {
            Path child = current.resolve(part);
            if (!Files.isRegularFile(child.resolve("memory.max"))) {
                Path controllers = current.resolve("cgroup.controllers");
                Path control = current.resolve("cgroup.subtree_control");
                if (!Files.isRegularFile(controllers)
                        || !read(controllers).matches("(?s).*\\bmemory\\b.*") || !Files.isRegularFile(control)) {
                    throw new ResourceControlException(String.format(
                            "Memory controller cannot be delegated below control group[%s]", current));
                }
                commands.write(control, "+memory");
            }
            current = child;
        }
    }

    @Override
    Long readMemoryLimit(Path target) {
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

    @Override
    Long readCpuTime(Path relative) {
        Path stat = root.resolve(relative).resolve("cpu.stat");
        if (!Files.isRegularFile(stat)) {
            return null;
        }
        for (String line : read(stat).split("\\R")) {
            String[] fields = line.trim().split("\\s+");
            if (fields.length == 2 && "usage_usec".equals(fields[0])) {
                return Math.multiplyExact(parseMemoryLimit(fields[1]), NANOSECONDS_PER_MICROSECOND);
            }
        }
        return null;
    }

    @Override
    long applyMemory(Path memoryTarget, long desiredLimit, Path cpuTarget) {
        boolean managed = managedGroup(root, memoryTarget);
        if (!Files.isDirectory(memoryTarget)) {
            if (managed && desiredLimit == 0) {
                return 0L;
            }
            if (!managed || desiredLimit == 0) {
                throw new ResourceControlException(String.format(
                        "Memory controller is unavailable for control group[%s]", memoryTarget));
            }
            mkdir(memoryTarget);
        }
        enableMemoryController(memoryTarget);
        Path limit = memoryTarget.resolve("memory.max");
        if (!Files.isRegularFile(limit)) {
            throw new ResourceControlException(String.format(
                    "Memory controller is unavailable for control group[%s]", memoryTarget));
        }
        if (managed && !memoryTarget.equals(cpuTarget) && desiredLimit > 0) {
            moveProcesses(cpuTarget.resolve("cgroup.procs"), memoryTarget.resolve("cgroup.procs"));
        }
        String desired = desiredLimit == 0 ? "max" : String.valueOf(desiredLimit);
        if (!desired.equals(read(limit).trim())) {
            validateMemoryLimitAgainstUsage(memoryTarget.resolve("memory.current"), desiredLimit);
            commands.write(limit, desired);
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

    void makePartitionRoot(Path target, String desired) {
        Path partition = target.resolve("cpuset.cpus.partition");
        Path exclusive = target.resolve("cpuset.cpus.exclusive");
        if (Files.isRegularFile(exclusive) && !desired.equals(normalizeOptional(read(exclusive)))) {
            commands.write(exclusive, desired);
        }
        if (!"root".equals(read(partition).trim())) {
            commands.write(partition, "root");
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

    void makePartitionMember(Path target) {
        Path partition = target.resolve("cpuset.cpus.partition");
        if (!Files.isRegularFile(partition)) {
            return;
        }
        if (!"member".equals(read(partition).trim())) {
            commands.write(partition, "member");
        }
        if (!"member".equals(read(partition).trim())) {
            throw new ResourceControlException(String.format(
                    "Failed to return control group[%s] to the shared CPU partition", target));
        }
        Path exclusive = target.resolve("cpuset.cpus.exclusive");
        if (Files.isRegularFile(exclusive) && !normalizeOptional(read(exclusive)).isEmpty()) {
            commands.write(exclusive, "\n");
        }
    }
}
