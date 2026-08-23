package org.zstack.header.physicalserver;

import java.util.ArrayList;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class PhysicalServerCpuTopology {
    public static class CoreGroup {
        private final String numaId;
        private final SortedSet<Integer> cpus;

        CoreGroup(String numaId, Collection<Integer> cpus) {
            this.numaId = numaId;
            this.cpus = new TreeSet<>(cpus);
        }

        public String getNumaId() {
            return numaId;
        }

        public SortedSet<Integer> getCpus() {
            return new TreeSet<>(cpus);
        }

        int firstCpu() {
            return cpus.first();
        }
    }

    private final SortedSet<Integer> onlineCpus;
    private final List<CoreGroup> coreGroups;

    private PhysicalServerCpuTopology(SortedSet<Integer> onlineCpus, List<CoreGroup> coreGroups) {
        this.onlineCpus = onlineCpus;
        this.coreGroups = coreGroups;
    }

    public static PhysicalServerCpuTopology from(Map<String, PhysicalServerNumaNode> numaNodes) {
        if (numaNodes == null || numaNodes.isEmpty()) {
            throw new IllegalArgumentException("CPU_TOPOLOGY_UNAVAILABLE: NUMA topology is empty");
        }

        SortedSet<Integer> online = new TreeSet<>();
        List<CoreGroup> groups = new ArrayList<>();
        Set<Integer> grouped = new HashSet<>();
        Map<String, PhysicalServerNumaNode> sortedNodes = new LinkedHashMap<>();
        numaNodes.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> sortedNodes.put(entry.getKey(), entry.getValue()));

        for (Map.Entry<String, PhysicalServerNumaNode> entry : sortedNodes.entrySet()) {
            PhysicalServerNumaNode node = entry.getValue();
            if (node.getOnlineCpus() == null || node.getCoreGroups() == null || node.getCoreGroups().isEmpty()) {
                throw new IllegalArgumentException(
                        "CPU_TOPOLOGY_UNSUPPORTED: topology provider did not report online CPUs and core sibling groups");
            }

            Set<Integer> nodeOnline = parseCpuIds(node.getOnlineCpus());
            online.addAll(nodeOnline);
            for (List<String> rawGroup : node.getCoreGroups()) {
                Set<Integer> group = parseCpuIds(rawGroup);
                if (group.isEmpty() || !nodeOnline.containsAll(group)) {
                    throw new IllegalArgumentException("CPU_TOPOLOGY_INVALID: core group is outside its NUMA online CPUs");
                }
                for (Integer cpu : group) {
                    if (!grouped.add(cpu)) {
                        throw new IllegalArgumentException(String.format(
                                "CPU_TOPOLOGY_INVALID: CPU[%s] appears in multiple core groups", cpu));
                    }
                }
                groups.add(new CoreGroup(entry.getKey(), group));
            }
        }

        if (!grouped.equals(online)) {
            throw new IllegalArgumentException("CPU_TOPOLOGY_INVALID: core groups do not cover every online CPU exactly once");
        }
        groups.sort(Comparator.comparingInt(CoreGroup::firstCpu));
        return new PhysicalServerCpuTopology(online, groups);
    }

    private static Set<Integer> parseCpuIds(List<String> values) {
        Set<Integer> result = new HashSet<>();
        for (String value : values) {
            if (value == null || !value.matches("[0-9]+")) {
                throw new IllegalArgumentException(String.format("CPU_TOPOLOGY_INVALID: invalid CPU id[%s]", value));
            }
            BigInteger cpu = new BigInteger(value);
            if (cpu.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0) {
                throw new IllegalArgumentException(String.format("CPU_TOPOLOGY_INVALID: CPU id[%s] is too large", value));
            }
            result.add(cpu.intValue());
        }
        return result;
    }

    public SortedSet<Integer> getOnlineCpus() {
        return new TreeSet<>(onlineCpus);
    }

    public List<CoreGroup> getCoreGroups() {
        return new ArrayList<>(coreGroups);
    }

    public CoreGroup getCpuZeroGroup() {
        for (CoreGroup group : coreGroups) {
            if (group.cpus.contains(0)) {
                return group;
            }
        }
        throw new IllegalArgumentException("CPU_TOPOLOGY_INVALID: online topology does not contain CPU0");
    }

    public String fingerprint() {
        List<String> encoded = new ArrayList<>();
        for (CoreGroup group : coreGroups) {
            encoded.add(group.numaId + ":" + PhysicalServerCpuSet.format(group.cpus));
        }
        return String.join(";", encoded);
    }
}
