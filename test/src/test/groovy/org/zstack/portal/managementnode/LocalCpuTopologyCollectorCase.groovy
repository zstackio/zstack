package org.zstack.portal.managementnode

import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.zstack.header.physicalserver.PhysicalServerCpuTopology

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

import static groovy.test.GroovyAssert.shouldFail

class LocalCpuTopologyCollectorCase {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder()

    @Test
    void testCollectsNumaAndSmtTopologyFromSysfsFacts() {
        Path root = temporaryFolder.newFolder("numa-topology").toPath()
        Path cpu = root.resolve("cpu")
        Path node = root.resolve("node")
        write(cpu.resolve("online"), "0-3\n")
        write(cpu.resolve("cpu0/topology/thread_siblings_list"), "0,2\n")
        write(cpu.resolve("cpu2/topology/thread_siblings_list"), "0,2\n")
        write(cpu.resolve("cpu1/topology/thread_siblings_list"), "1,3\n")
        write(cpu.resolve("cpu3/topology/thread_siblings_list"), "1,3\n")
        write(node.resolve("node0/cpulist"), "0,2\n")
        write(node.resolve("node1/cpulist"), "1,3\n")

        PhysicalServerCpuTopology topology = new LocalCpuTopologyCollector(cpu, node).collect()
        def actual = topology.coreGroups.collect {
            [it.numaId, it.cpus]
        }
        def expected = [["0", [0, 2] as Set], ["1", [1, 3] as Set]]
        assert actual == expected :
                "sysfs NUMA and thread-sibling facts must become stable core groups: " +
                        "expected=${expected} actual=${actual}"
    }

    @Test
    void testFallsBackToOneNumaNodeWhenNodeSysfsIsAbsent() {
        Path root = temporaryFolder.newFolder("flat-topology").toPath()
        Path cpu = root.resolve("cpu")
        Path missingNodeRoot = root.resolve("missing-node")
        write(cpu.resolve("online"), "0-1\n")
        write(cpu.resolve("cpu0/topology/thread_siblings_list"), "0-1\n")
        write(cpu.resolve("cpu1/topology/thread_siblings_list"), "0-1\n")

        PhysicalServerCpuTopology topology = new LocalCpuTopologyCollector(cpu, missingNodeRoot).collect()
        def actual = topology.coreGroups.collect {
            [it.numaId, it.cpus]
        }
        def expected = [["0", [0, 1] as Set]]
        assert actual == expected :
                "a machine without NUMA node directories must still expose one complete topology: " +
                        "expected=${expected} actual=${actual}"
    }

    @Test
    void testRejectsAnEmptySiblingFact() {
        Path root = temporaryFolder.newFolder("invalid-topology").toPath()
        Path cpu = root.resolve("cpu")
        write(cpu.resolve("online"), "0\n")
        write(cpu.resolve("cpu0/topology/thread_siblings_list"), "\n")

        Throwable failure = shouldFail {
            new LocalCpuTopologyCollector(cpu, root.resolve("missing-node")).collect()
        }
        assert failure?.message?.contains("CPU list is empty") :
                "empty sysfs facts must explain the invalid topology: " + "actual=${failure}"
    }

    private static void write(Path path, String value) {
        Files.createDirectories(path.parent)
        Files.write(path, value.getBytes(StandardCharsets.US_ASCII))
    }

}
