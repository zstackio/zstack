package org.zstack.physicalserver;

import org.zstack.header.physicalserver.PhysicalServerCpuSet;
import org.zstack.header.physicalserver.PhysicalServerCpuTopology;
import org.zstack.header.physicalserver.PhysicalServerResourceIsolationMode;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;

public class PhysicalServerCpuPlanner {
    public String validateAndNormalize(
            PhysicalServerResourceIsolationMode isolationMode,
            String cpuSet,
            PhysicalServerCpuTopology topology,
            Collection<Integer> allocatedExclusiveCpus) {
        SortedSet<Integer> desired = PhysicalServerCpuSet.parse(
                cpuSet, topology.getOnlineCpus());
        if (!disjoint(desired, allocatedExclusiveCpus)) {
            throw new IllegalArgumentException(String.format(
                    "CPU_SET_CONFLICT: %s CPU set overlaps an exclusive role",
                    isolationMode == PhysicalServerResourceIsolationMode.EXCLUSIVE
                            ? "exclusive" : "shared"));
        }
        if (isolationMode == PhysicalServerResourceIsolationMode.EXCLUSIVE) {
            validateWholeCores(desired, topology);
        }
        return PhysicalServerCpuSet.format(desired);
    }

    private void validateWholeCores(
            Set<Integer> desired,
            PhysicalServerCpuTopology topology) {
        for (PhysicalServerCpuTopology.CoreGroup group : topology.getCoreGroups()) {
            boolean intersects = !disjoint(desired, group.getCpus());
            if (intersects && !desired.containsAll(group.getCpus())) {
                throw new IllegalArgumentException(String.format(
                        "CPU_SIBLING_SPLIT: CPU_SET splits core group[%s]",
                        PhysicalServerCpuSet.format(group.getCpus())));
            }
        }
        if (!disjoint(desired, topology.getCpuZeroGroup().getCpus())) {
            throw new IllegalArgumentException(
                    "CPU_ZERO_RESERVED: CPU0 core group must remain shared");
        }
    }

    private boolean disjoint(
            Collection<Integer> left, Collection<Integer> right) {
        Set<Integer> copy = new HashSet<>(left);
        copy.retainAll(right);
        return copy.isEmpty();
    }
}
