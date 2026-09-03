package org.zstack.physicalserver;

import org.zstack.header.physicalserver.PhysicalServerCpuSet;
import org.zstack.header.physicalserver.PhysicalServerCpuTopology;
import org.zstack.header.physicalserver.PhysicalServerResourceAssignmentObserver;
import org.zstack.header.physicalserver.PhysicalServerResourceIsolationMode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class PhysicalServerCpuPlanner {
    public Set<Integer> calculateAllocatedExclusiveCpus(
            PhysicalServerResourceAssignmentVO current,
            Collection<PhysicalServerResourceAssignmentVO> assignments,
            PhysicalServerResourceExtensionRegistry extensions, PhysicalServerCpuTopology topology) {
        Set<Integer> result = new HashSet<>();
        for (PhysicalServerResourceAssignmentVO assignment : assignments) {
            if (current.getUuid().equals(assignment.getUuid())) {
                continue;
            }
            PhysicalServerResourceAssignmentObserver observer = extensions.observer(assignment.getRoleType());
            if (observer == null
                    || observer.getIsolationMode()
                    != PhysicalServerResourceIsolationMode.EXCLUSIVE || empty(assignment.getCpuSet())) {
                continue;
            }
            result.addAll(PhysicalServerCpuSet.parse(assignment.getCpuSet(), topology.getOnlineCpus()));
        }
        return result;
    }

    public String calculateDefaultCpuSet(
            Integer defaultCpuCount, PhysicalServerCpuTopology topology, Set<Integer> allocatedExclusiveCpus) {
        if (defaultCpuCount == null || defaultCpuCount < 1) {
            throw new IllegalArgumentException("Default CPU count must be greater than zero");
        }
        return PhysicalServerCpuSet
                .firstAvailableExcludingCpuZeroCore(topology, allocatedExclusiveCpus, defaultCpuCount);
    }

    public String matchProfileCpuCount(
            Integer defaultCpuCount,
            String currentCpuSet,
            PhysicalServerResourceIsolationMode isolationMode,
            PhysicalServerCpuTopology topology, Set<Integer> allocatedExclusiveCpus) {
        if (defaultCpuCount == null) {
            return empty(currentCpuSet) ? "" : validateAndNormalize(
                    isolationMode, currentCpuSet, topology, allocatedExclusiveCpus);
        }
        if (defaultCpuCount < 1) {
            throw new IllegalArgumentException("Default CPU count must be greater than zero");
        }
        if (empty(currentCpuSet)) {
            return isolationMode == PhysicalServerResourceIsolationMode.EXCLUSIVE
                    ? firstAvailableExclusiveCpuSet(topology, allocatedExclusiveCpus, defaultCpuCount)
                    : calculateDefaultCpuSet(defaultCpuCount, topology, allocatedExclusiveCpus);
        }

        SortedSet<Integer> current = PhysicalServerCpuSet.parse(
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

    private SortedSet<Integer> resizeSharedCpuSet(
            SortedSet<Integer> current,
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
        for (PhysicalServerCpuTopology.CoreGroup group : preferredCoreGroups(current, topology)) {
            for (Integer cpu : group.getCpus()) {
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

    private SortedSet<Integer> resizeExclusiveCpuSet(
            SortedSet<Integer> current,
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

        for (PhysicalServerCpuTopology.CoreGroup group : preferredCoreGroups(current, topology)) {
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

    private String firstAvailableExclusiveCpuSet(
            PhysicalServerCpuTopology topology, Collection<Integer> unavailable, int targetCount) {
        Set<String> numaIds = numaIds(topology.getOnlineCpus(), topology);
        for (String numaId : numaIds) {
            SortedSet<Integer> selected = new TreeSet<>();
            for (PhysicalServerCpuTopology.CoreGroup group : topology.getCoreGroups()) {
                Set<Integer> cpus = group.getCpus();
                if (!numaId.equals(group.getNumaId())
                        || !Collections.disjoint(cpus, unavailable)
                        || !Collections.disjoint(cpus, topology.getCpuZeroGroup().getCpus())
                        || selected.size() + cpus.size() > targetCount) {
                    continue;
                }
                selected.addAll(cpus);
                if (selected.size() == targetCount) {
                    return PhysicalServerCpuSet.format(selected);
                }
            }
        }
        throw exclusiveCountUnavailable(targetCount);
    }

    private List<PhysicalServerCpuTopology.CoreGroup> preferredCoreGroups(
            Set<Integer> current, PhysicalServerCpuTopology topology) {
        Set<String> preferredNumaIds = numaIds(current, topology);
        for (PhysicalServerCpuTopology.CoreGroup group : topology.getCoreGroups()) {
            preferredNumaIds.add(group.getNumaId());
        }
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

    public String validateAndNormalize(
            PhysicalServerResourceIsolationMode isolationMode,
            String cpuSet, PhysicalServerCpuTopology topology, Collection<Integer> allocatedExclusiveCpus) {
        SortedSet<Integer> desired = PhysicalServerCpuSet.parse(cpuSet, topology.getOnlineCpus());
        if (!Collections.disjoint(desired, allocatedExclusiveCpus)) {
            throw new IllegalArgumentException(String.format(
                    "%s CPU set overlaps an exclusive role",
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
