package org.zstack.header.physicalserver;

import java.util.Collection;

public class PhysicalServerResourceBoundary {
    private String cpuSet;
    private Long memory;
    private boolean synced = true;

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

    public boolean isSynced() {
        return synced;
    }

    public void setSynced(boolean synced) {
        this.synced = synced;
    }

    public static PhysicalServerResourceBoundary fromManagedServiceUsages(
            Collection<ManagedServiceResourceUsage> services) {
        String cpuSet = "";
        Long memory = null;
        boolean synced = true;
        if (services != null) {
            for (ManagedServiceResourceUsage service : services) {
                if (service.isRestartRequired()) {
                    synced = false;
                }
                if (service.getCpuSet() != null && !service.getCpuSet().trim().isEmpty()) {
                    cpuSet = PhysicalServerCpuSet.union(cpuSet, service.getCpuSet());
                }
                if (service.getMemoryLimit() == null) {
                    continue;
                }
                if (memory != null && !memory.equals(service.getMemoryLimit())) {
                    throw new IllegalStateException(
                            "Managed services report different memory limits for the same role");
                }
                memory = service.getMemoryLimit();
            }
        }
        PhysicalServerResourceBoundary boundary = new PhysicalServerResourceBoundary();
        boundary.setCpuSet(cpuSet);
        boundary.setMemory(memory);
        boundary.setSynced(synced);
        return boundary;
    }
}
