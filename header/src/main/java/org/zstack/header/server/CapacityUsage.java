package org.zstack.header.server;

public class CapacityUsage {
    private long usedCpu;
    private long usedMemory;
    /**
     * True for SchedulingMode.INTERNAL_EXCLUSIVE roles (e.g. BAREMETAL_V2) to signal the
     * allocator that the entire physical server is claimed regardless of
     * {@code usedCpu/usedMemory} magnitudes. Consumers must zero
     * {@code availableCpu/availableMemory} when {@code exclusive=true}.
     */
    private boolean exclusive;

    public long getUsedCpu() { return usedCpu; }
    public void setUsedCpu(long usedCpu) { this.usedCpu = usedCpu; }
    public long getUsedMemory() { return usedMemory; }
    public void setUsedMemory(long usedMemory) { this.usedMemory = usedMemory; }
    public boolean isExclusive() { return exclusive; }
    public void setExclusive(boolean exclusive) { this.exclusive = exclusive; }
}
