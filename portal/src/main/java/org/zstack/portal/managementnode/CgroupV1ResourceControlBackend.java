package org.zstack.portal.managementnode;

import org.zstack.portal.managementnode.ResourceControlUtils.CommandExecutor;
import org.zstack.portal.managementnode.ResourceControlUtils.ResourceControlException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.zstack.portal.managementnode.ResourceControlUtils.read;
import static org.zstack.portal.managementnode.ResourceControlUtils.underRoot;

class CgroupV1ResourceControlBackend extends CgroupResourceControlBackend {
    CgroupV1ResourceControlBackend(Path root, CommandExecutor commandExecutor) {
        super(root, commandExecutor);
    }

    @Override
    boolean supportsSubtreeMemoryLimit(Path target) {
        Path hierarchy = target.resolve("memory.use_hierarchy");
        return Files.isRegularFile(hierarchy) && "1".equals(read(hierarchy).trim());
    }

    @Override
    boolean supportsExclusiveCpuPartition() {
        return false;
    }

    @Override
    String cpuSetProperty() {
        return null;
    }

    @Override
    String memoryLimitProperty() {
        return "MemoryLimit";
    }

    @Override
    String memoryUsageFile() {
        return "memory.usage_in_bytes";
    }

    @Override
    boolean matchesCpuHierarchy(String[] fields) {
        return Arrays.asList(fields[1].split(",")).contains("cpuset");
    }

    @Override
    void enableCpuController(Path target) {
    }

    @Override
    void requireCpuTarget(Path target) {
    }

    @Override
    String applyExclusiveCpuSet(Path target, String desired) {
        throw new ResourceControlException("Exclusive CPU partitions are not supported by the cgroup backend");
    }

    @Override
    void releaseCpuSet(Path target) {
        initializeMems(target);
        resetCpuSet(target, parentCpuSet(target));
    }

    @Override
    Long readCpuTime(Path relative) {
        Path usage = root.resolve(relative).resolve("cpuacct.usage");
        return Files.isRegularFile(usage) ? parseMemoryLimit(read(usage).trim()) : null;
    }

    @Override
    void initializeCpus(Path target) {
        Path cpus = target.resolve("cpuset.cpus");
        if (read(cpus).trim().isEmpty()) {
            commands.write(cpus, parentCpuSet(target));
        }
    }

    @Override
    Long readMemoryLimit(Path target) {
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

    @Override
    long applyMemory(Path memoryTarget, long desiredLimit, Path cpuTarget) {
        boolean managed = managedGroup(root, memoryTarget);
        Path rootLimit = root.resolve("memory.limit_in_bytes");
        if (!Files.isRegularFile(rootLimit)) {
            throw new ResourceControlException("Cgroup v1 memory controller does not expose its root limit");
        }
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
        Path limit = memoryTarget.resolve("memory.limit_in_bytes");
        if (!Files.isRegularFile(limit)) {
            throw new ResourceControlException(String.format(
                    "Memory controller is unavailable for control group[%s]", memoryTarget));
        }
        if (managed && desiredLimit > 0) {
            moveProcesses(cpuTarget.resolve("cgroup.procs"), memoryTarget.resolve("cgroup.procs"));
        }
        String desired = desiredLimit == 0 ? read(rootLimit).trim() : String.valueOf(desiredLimit);
        if (!desired.equals(read(limit).trim())) {
            validateMemoryLimitAgainstUsage(memoryTarget.resolve("memory.usage_in_bytes"), desiredLimit);
            commands.write(limit, desired);
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
}
