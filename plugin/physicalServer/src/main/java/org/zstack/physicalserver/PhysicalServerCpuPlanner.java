package org.zstack.physicalserver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import org.zstack.header.physicalserver.PhysicalServerCpuSet;
import org.zstack.header.physicalserver.PhysicalServerCpuTopology;
import org.zstack.header.physicalserver.PhysicalServerResourceIsolationMode;

public class PhysicalServerCpuPlanner {
    public Set<Integer> calculateAllocatedExclusiveCpus(PhysicalServerResourceAssignmentVO current,
            Collection<PhysicalServerResourceAssignmentVO> assignments,
            Map<String, PhysicalServerResourceIsolationMode> isolationModes, PhysicalServerCpuTopology topology) {
        Set<Integer> result = new HashSet<>();
        for (PhysicalServerResourceAssignmentVO assignment : assignments) {
            if (current.getUuid().equals(assignment.getUuid())) {
                continue;
            }
            if (isolationModes.get(assignment.getRoleType()) != PhysicalServerResourceIsolationMode.EXCLUSIVE
                    || empty(assignment.getCpuSet())) {
                continue;
            }
            result.addAll(PhysicalServerCpuSet.parse(assignment.getCpuSet(), topology.getOnlineCpus()));
        }
        return result;
    }

    public String matchProfileCpuCount(Integer defaultCpuCount,
            String currentCpuSet, PhysicalServerResourceIsolationMode isolationMode,
            PhysicalServerCpuTopology topology, Set<Integer> allocatedExclusiveCpus) {
        if (defaultCpuCount == null) {
            return empty(currentCpuSet) ? "" : validateAndNormalize(
                    isolationMode, currentCpuSet, topology, allocatedExclusiveCpus);
        }
        if (defaultCpuCount < 1) {
            throw new IllegalArgumentException("Default CPU count must be greater than zero");
        }
        SortedSet<Integer> current = empty(currentCpuSet) ? new TreeSet<>() : PhysicalServerCpuSet.parse(
                validateAndNormalize(isolationMode, currentCpuSet, topology, allocatedExclusiveCpus),
                topology.getOnlineCpus());
        if (current.size() == defaultCpuCount) {
            return PhysicalServerCpuSet.format(current);
        }
        SortedSet<Integer> resized = isolationMode
                == PhysicalServerResourceIsolationMode.EXCLUSIVE
                ? resizeExclusiveCpuSet(current, defaultCpuCount, topology, allocatedExclusiveCpus)
                : resizeSharedCpuSet(current, defaultCpuCount, topology, allocatedExclusiveCpus);
        return validateAndNormalize(
                isolationMode, PhysicalServerCpuSet.format(resized), topology, allocatedExclusiveCpus);
    }

    private SortedSet<Integer> resizeSharedCpuSet(SortedSet<Integer> current,
            int targetCount, PhysicalServerCpuTopology topology, Collection<Integer> unavailable) {
        SortedSet<Integer> result = new TreeSet<>(current);
        while (result.size() > targetCount) {
            result.remove(result.last());
        }
        if (result.size() == targetCount) {
            return result;
        }

        Set<Integer> excluded = new HashSet<>(unavailable);
        excluded.addAll(topology.getCpuZeroGroup().getCpus());
        Map<String, SortedSet<Integer>> cpusByNuma = new LinkedHashMap<>();
        for (PhysicalServerCpuTopology.CoreGroup group :
                preferredCoreGroups(current, targetCount - result.size(), topology, excluded)) {
            cpusByNuma.computeIfAbsent(group.getNumaId(), ignored -> new TreeSet<>()).addAll(group.getCpus());
        }
        for (SortedSet<Integer> cpus : cpusByNuma.values()) {
            for (Integer cpu : cpus) {
                if (!excluded.contains(cpu)) {
                    result.add(cpu);
                }
                if (result.size() == targetCount) {
                    return result;
                }
            }
        }
        return result;
    }

    private SortedSet<Integer> resizeExclusiveCpuSet(SortedSet<Integer> current,
            int targetCount, PhysicalServerCpuTopology topology, Collection<Integer> unavailable) {
        SortedSet<Integer> result = new TreeSet<>(current);
        List<PhysicalServerCpuTopology.CoreGroup> currentGroups = new ArrayList<>();
        for (PhysicalServerCpuTopology.CoreGroup group : topology.getCoreGroups()) {
            if (current.containsAll(group.getCpus())) {
                currentGroups.add(group);
            }
        }
        for (int index = currentGroups.size() - 1; result.size() > targetCount && index >= 0; index--) {
            Set<Integer> group = currentGroups.get(index).getCpus();
            if (result.size() - group.size() >= targetCount) {
                result.removeAll(group);
            }
        }
        if (result.size() == targetCount) {
            return result;
        }
        if (result.size() > targetCount) {
            throw exclusiveCountUnavailable(targetCount);
        }

        Set<Integer> excluded = new HashSet<>(unavailable);
        for (PhysicalServerCpuTopology.CoreGroup group : topology.getCoreGroups()) {
            if (!Collections.disjoint(group.getCpus(), unavailable)) {
                excluded.addAll(group.getCpus());
            }
        }
        for (PhysicalServerCpuTopology.CoreGroup group :
                preferredCoreGroups(current, targetCount - result.size(), topology, excluded)) {
            Set<Integer> cpus = group.getCpus();
            if (current.containsAll(cpus)
                    || !Collections.disjoint(cpus, unavailable)
                    || !Collections.disjoint(cpus, topology.getCpuZeroGroup().getCpus())
                    || result.size() + cpus.size() > targetCount) {
                continue;
            }
            result.addAll(cpus);
            if (result.size() == targetCount) {
                return result;
            }
        }
        throw exclusiveCountUnavailable(targetCount);
    }

    private List<PhysicalServerCpuTopology.CoreGroup> preferredCoreGroups(
            Set<Integer> current, int additionalCount, PhysicalServerCpuTopology topology,
            Collection<Integer> unavailable) {
        Set<String> preferredNumaIds = numaIds(current, topology);
        Map<String, Set<Integer>> availableByNuma = new LinkedHashMap<>();
        for (PhysicalServerCpuTopology.CoreGroup group : topology.getCoreGroups()) {
            availableByNuma.computeIfAbsent(group.getNumaId(), ignored -> new HashSet<>()).addAll(group.getCpus());
        }
        availableByNuma.forEach((numaId, cpus) -> {
            cpus.removeAll(current);
            cpus.removeAll(unavailable);
            cpus.removeAll(topology.getCpuZeroGroup().getCpus());
            if (cpus.size() >= additionalCount) {
                preferredNumaIds.add(numaId);
            }
        });
        preferredNumaIds.addAll(availableByNuma.keySet());
        List<PhysicalServerCpuTopology.CoreGroup> result = new ArrayList<>();
        for (String numaId : preferredNumaIds) {
            for (PhysicalServerCpuTopology.CoreGroup group : topology.getCoreGroups()) {
                if (numaId.equals(group.getNumaId())) {
                    result.add(group);
                }
            }
        }
        return result;
    }

    private Set<String> numaIds(Set<Integer> cpus, PhysicalServerCpuTopology topology) {
        Set<String> result = new LinkedHashSet<>();
        for (PhysicalServerCpuTopology.CoreGroup group : topology.getCoreGroups()) {
            if (!Collections.disjoint(group.getCpus(), cpus)) {
                result.add(group.getNumaId());
            }
        }
        return result;
    }

    private IllegalArgumentException exclusiveCountUnavailable(int targetCount) {
        return new IllegalArgumentException(String.format(
                "%s logical CPUs cannot be allocated as complete cores", targetCount));
    }

    public String validateAndNormalize(PhysicalServerResourceIsolationMode isolationMode,
            String cpuSet, PhysicalServerCpuTopology topology, Collection<Integer> allocatedExclusiveCpus) {
        SortedSet<Integer> desired = PhysicalServerCpuSet.parse(cpuSet, topology.getOnlineCpus());
        if (!Collections.disjoint(desired, allocatedExclusiveCpus)) {
            throw new IllegalArgumentException(String.format("%s CPU set overlaps an exclusive role",
                    isolationMode == PhysicalServerResourceIsolationMode.EXCLUSIVE ? "exclusive" : "shared"));
        }
        if (isolationMode == PhysicalServerResourceIsolationMode.EXCLUSIVE) {
            validateWholeCores(desired, topology);
        }
        return PhysicalServerCpuSet.format(desired);
    }

    private void validateWholeCores(Set<Integer> desired, PhysicalServerCpuTopology topology) {
        for (PhysicalServerCpuTopology.CoreGroup group : topology.getCoreGroups()) {
            boolean intersects = !Collections.disjoint(desired, group.getCpus());
            if (intersects && !desired.containsAll(group.getCpus())) {
                throw new IllegalArgumentException(String.format(
                        "CPU set splits core group[%s]", PhysicalServerCpuSet.format(group.getCpus())));
            }
        }
        if (!Collections.disjoint(desired, topology.getCpuZeroGroup().getCpus())) {
            throw new IllegalArgumentException("CPU0 core group must remain shared");
        }
    }

    private boolean empty(String value) {
        return value == null || value.trim().isEmpty();
    }
}
