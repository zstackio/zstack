package org.zstack.portal.managementnode;

import org.zstack.portal.managementnode.ResourceControlUtils.CommandExecutor;
import org.zstack.portal.managementnode.ResourceControlUtils.ResourceControlException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.zstack.portal.managementnode.ResourceControlUtils.read;

class CgroupResourceControlBackendFactory {
    private final CommandExecutor commandExecutor;
    private final Path v2Root;
    private final Path v1Root;
    private final Path v1MemoryRoot;
    private final List<Path> v1CpuacctRoots;
    private final Path procMounts;

    CgroupResourceControlBackendFactory(Path v2Root, Path v1Root, Path v1MemoryRoot,
            List<Path> v1CpuacctRoots, Path procMounts, CommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
        this.v2Root = v2Root;
        this.v1Root = v1Root;
        this.v1MemoryRoot = v1MemoryRoot;
        this.v1CpuacctRoots = new ArrayList<>(v1CpuacctRoots);
        this.procMounts = procMounts;
    }

    CgroupResourceControlBackend cpu() {
        for (Path root : v2Roots()) {
            Path controllers = root.resolve("cgroup.controllers");
            String values = read(controllers);
            if (values.matches("(?s).*\\bcpuset\\b.*")
                    || Files.isRegularFile(root.resolve("cpuset.cpus.effective"))) {
                return new CgroupV2ResourceControlBackend(root, commandExecutor);
            }
        }
        if (Files.isRegularFile(v1Root.resolve("cpuset.cpus"))) {
            return new CgroupV1ResourceControlBackend(v1Root, commandExecutor);
        }
        throw new ResourceControlException("No available cpuset controller was found");
    }

    CgroupResourceControlBackend memory() {
        CgroupResourceControlBackend backend = findMemory();
        if (backend != null) {
            return backend;
        }
        throw new ResourceControlException("No available memory controller was found");
    }

    CgroupResourceControlBackend findMemory() {
        for (Path root : v2Roots()) {
            String controllers = read(root.resolve("cgroup.controllers"));
            if (controllers.matches("(?s).*\\bmemory\\b.*") || Files.isRegularFile(root.resolve("memory.max"))) {
                return new CgroupV2ResourceControlBackend(root, commandExecutor);
            }
        }
        if (Files.isRegularFile(v1MemoryRoot.resolve("memory.limit_in_bytes"))) {
            return new CgroupV1ResourceControlBackend(v1MemoryRoot, commandExecutor);
        }
        return null;
    }

    List<Path> v2Roots() {
        Set<Path> roots = new LinkedHashSet<>();
        if (Files.isRegularFile(v2Root.resolve("cgroup.controllers"))) {
            roots.add(v2Root);
        }
        if (!Files.isRegularFile(procMounts)) {
            return new ArrayList<>(roots);
        }
        for (String line : read(procMounts).split("\\R")) {
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

    List<CgroupResourceControlBackend> cpuAccounting() {
        List<CgroupResourceControlBackend> backends = new ArrayList<>();
        for (Path root : v2Roots()) {
            backends.add(new CgroupV2ResourceControlBackend(root, commandExecutor));
        }
        backends.addAll(legacyCpuAccounting());
        return backends;
    }

    List<CgroupResourceControlBackend> legacyCpuAccounting() {
        List<CgroupResourceControlBackend> backends = new ArrayList<>();
        for (Path root : v1CpuacctRoots) {
            backends.add(new CgroupV1ResourceControlBackend(root, commandExecutor));
        }
        return backends;
    }

    private String decodeMountPath(String value) {
        return value.replace("\\040", " ").replace("\\011", "\t").replace("\\012", "\n").replace("\\134", "\\");
    }

}
