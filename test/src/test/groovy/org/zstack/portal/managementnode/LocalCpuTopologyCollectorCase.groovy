package org.zstack.portal.managementnode

import org.junit.Test
import org.zstack.header.physicalserver.PhysicalServerCpuTopology

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

class LocalCpuTopologyCollectorCase {
    @Test
    void testCollectsNumaAndSmtTopologyFromSysfsFacts() {
        Path root = Files.createTempDirectory("local-cpu-topology-case-")
        try {
            Path cpu = root.resolve("cpu")
            Path node = root.resolve("node")
            write(cpu.resolve("online"), "0-3\n")
            write(cpu.resolve("cpu0/topology/thread_siblings_list"), "0,2\n")
            write(cpu.resolve("cpu2/topology/thread_siblings_list"), "0,2\n")
            write(cpu.resolve("cpu1/topology/thread_siblings_list"), "1,3\n")
            write(cpu.resolve("cpu3/topology/thread_siblings_list"), "1,3\n")
            write(node.resolve("node0/cpulist"), "0,2\n")
            write(node.resolve("node1/cpulist"), "1,3\n")

            PhysicalServerCpuTopology topology =
                    new LocalCpuTopologyCollector(cpu, node).collect()
            assert topology.fingerprint() == "0:0,2;1:1,3" :
                    "sysfs NUMA and thread-sibling facts must become stable core groups: " +
                            "expected=0:0,2;1:1,3 actual=${topology.fingerprint()}"
        } finally {
            removeTree(root)
        }
    }

    @Test
    void testFallsBackToOneNumaNodeWhenNodeSysfsIsAbsent() {
        Path root = Files.createTempDirectory("local-cpu-flat-topology-case-")
        try {
            Path cpu = root.resolve("cpu")
            Path missingNodeRoot = root.resolve("missing-node")
            write(cpu.resolve("online"), "0-1\n")
            write(cpu.resolve("cpu0/topology/thread_siblings_list"), "0-1\n")
            write(cpu.resolve("cpu1/topology/thread_siblings_list"), "0-1\n")

            PhysicalServerCpuTopology topology =
                    new LocalCpuTopologyCollector(
                            cpu, missingNodeRoot).collect()
            assert topology.fingerprint() == "0:0-1" :
                    "a machine without NUMA node directories must still expose one complete topology: " +
                            "expected=0:0-1 actual=${topology.fingerprint()}"
        } finally {
            removeTree(root)
        }
    }

    @Test
    void testRejectsAnEmptySiblingFact() {
        Path root = Files.createTempDirectory("local-cpu-invalid-topology-case-")
        try {
            Path cpu = root.resolve("cpu")
            write(cpu.resolve("online"), "0\n")
            write(cpu.resolve("cpu0/topology/thread_siblings_list"), "\n")

            Throwable failure = null
            try {
                new LocalCpuTopologyCollector(
                        cpu, root.resolve("missing-node")).collect()
            } catch (Throwable error) {
                failure = error
            }
            assert failure?.message?.contains("CPU_TOPOLOGY_INVALID") :
                    "empty sysfs facts must fail with a typed topology reason: " +
                            "actual=${failure}"
        } finally {
            removeTree(root)
        }
    }

    private static void write(Path path, String value) {
        Files.createDirectories(path.parent)
        Files.write(path, value.getBytes(StandardCharsets.US_ASCII))
    }

    private static void removeTree(Path root) {
        if (!Files.exists(root)) {
            return
        }
        Files.walk(root).sorted(Comparator.reverseOrder()).forEach {
            Files.deleteIfExists(it)
        }
    }
}
