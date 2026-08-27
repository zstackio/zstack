package org.zstack.header.physicalserver;

import java.util.Collection;

public class PhysicalServerResourceBoundary {
    private String cpuSet;
    private Long memory;

    public String getCpuSet() {
        return cpuSet;
    }

    public void setCpuSet(String cpuSet) {
        this.cpuSet = cpuSet;
    }

    public Long getMemory() {
        return memory;
    }

    public void setMemory(Long memory) {
        this.memory = memory;
    }

    public static PhysicalServerResourceBoundary fromManagedServiceUsages(
            Collection<ManagedServiceResourceUsage> services) {
        String cpuSet = "";
        Long memory = null;
        if (services != null) {
            for (ManagedServiceResourceUsage service : services) {
                if (service.getCpuSet() != null
                        && !service.getCpuSet().trim().isEmpty()) {
                    cpuSet = PhysicalServerCpuSet.union(
                            cpuSet, service.getCpuSet());
                }
                if (service.getMemoryLimit() == null) {
                    continue;
                }
                if (memory != null && !memory.equals(service.getMemoryLimit())) {
                    throw new IllegalStateException(
                            "RESOURCE_ASSIGNMENT_OBSERVATION_INVALID: service memory limits differ");
                }
                memory = service.getMemoryLimit();
            }
        }
        PhysicalServerResourceBoundary boundary =
                new PhysicalServerResourceBoundary();
        boundary.setCpuSet(cpuSet);
        boundary.setMemory(memory);
        return boundary;
    }
}
