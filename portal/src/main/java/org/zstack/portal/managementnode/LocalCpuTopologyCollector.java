package org.zstack.portal.managementnode;

import org.zstack.core.CoreGlobalProperty;
import org.zstack.header.physicalserver.PhysicalServerCpuSet;
import org.zstack.header.physicalserver.PhysicalServerCpuTopology;
import org.zstack.header.physicalserver.PhysicalServerNumaNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class LocalCpuTopologyCollector {
    private static final Path CPU_ROOT = Paths.get("/sys/devices/system/cpu");
    private static final Path NODE_ROOT = Paths.get("/sys/devices/system/node");
    private final Path cpuRoot;
    private final Path nodeRoot;
    private volatile PhysicalServerCpuTopology testTopology;

    public LocalCpuTopologyCollector() {
        this(CPU_ROOT, NODE_ROOT);
    }

    LocalCpuTopologyCollector(Path cpuRoot, Path nodeRoot) {
        this.cpuRoot = cpuRoot;
        this.nodeRoot = nodeRoot;
    }

    public PhysicalServerCpuTopology collect() {
        if (CoreGlobalProperty.UNIT_TEST_ON && testTopology != null) {
            return testTopology;
        }
        try {
            SortedSet<Integer> online = readCpuSet(cpuRoot.resolve("online"));
            List<Path> nodePaths = nodePaths();
            if (nodePaths.isEmpty()) {
                return PhysicalServerCpuTopology.from(Collections.singletonMap("0", node("0", online)));
            }

            Map<String, PhysicalServerNumaNode> result = new LinkedHashMap<>();
            for (Path nodePath : nodePaths) {
                String nodeId = nodePath.getFileName().toString().substring("node".length());
                SortedSet<Integer> cpus = readCpuSet(nodePath.resolve("cpulist"));
                SortedSet<Integer> nodeOnline = new TreeSet<>(cpus);
                nodeOnline.retainAll(online);
                if (!nodeOnline.isEmpty()) {
                    result.put(nodeId, node(nodeId, nodeOnline));
                }
            }
            if (result.isEmpty()) {
                throw new IllegalArgumentException("No online NUMA CPU was found");
            }
            return PhysicalServerCpuTopology.from(result);
        } catch (IOException exception) {
            throw new IllegalArgumentException(
                    "Failed to read local CPU topology: " + exception.getMessage(), exception);
        }
    }

    public void setTestTopology(PhysicalServerCpuTopology testTopology) {
        if (!CoreGlobalProperty.UNIT_TEST_ON) {
            throw new IllegalStateException("test topology is only available in unit-test mode");
        }
        this.testTopology = testTopology;
    }

    public void clearTestTopology() {
        testTopology = null;
    }

    private List<Path> nodePaths() throws IOException {
        if (!Files.isDirectory(nodeRoot)) {
            return Collections.emptyList();
        }
        List<Path> paths = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(nodeRoot, "node[0-9]*")) {
            for (Path path : stream) {
                if (path.getFileName().toString().matches("node[0-9]+")) {
                    paths.add(path);
                }
            }
        }
        paths.sort(Comparator.comparingInt(path -> Integer.parseInt(
                path.getFileName().toString().substring("node".length()))));
        return paths;
    }

    private PhysicalServerNumaNode node(String nodeId, SortedSet<Integer> online) throws IOException {
        PhysicalServerNumaNode node = new PhysicalServerNumaNode();
        node.setNodeId(nodeId);
        node.setOnlineCpus(strings(online));
        node.setCoreGroups(coreGroups(online));
        return node;
    }

    private List<List<String>> coreGroups(SortedSet<Integer> online) throws IOException {
        Set<SortedSet<Integer>> groups = new LinkedHashSet<>();
        for (Integer cpu : online) {
            Path siblings = cpuRoot.resolve(String.format("cpu%s/topology/thread_siblings_list", cpu));
            SortedSet<Integer> group = readCpuSet(siblings);
            group.retainAll(online);
            if (group.isEmpty()) {
                throw new IllegalArgumentException(String.format("CPU[%s] has no online core sibling", cpu));
            }
            groups.add(group);
        }
        List<SortedSet<Integer>> sorted = new ArrayList<>(groups);
        sorted.sort(Comparator.comparingInt(SortedSet::first));
        List<List<String>> result = new ArrayList<>();
        for (SortedSet<Integer> group : sorted) {
            result.add(strings(group));
        }
        return result;
    }

    private SortedSet<Integer> readCpuSet(Path path) throws IOException {
        String value = new String(Files.readAllBytes(path), StandardCharsets.US_ASCII).trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("CPU list is empty at " + path);
        }
        return PhysicalServerCpuSet.parse(value);
    }

    private List<String> strings(Set<Integer> cpus) {
        List<String> result = new ArrayList<>();
        for (Integer cpu : cpus) {
            result.add(String.valueOf(cpu));
        }
        return result;
    }
}
