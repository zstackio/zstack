package org.zstack.header.physicalserver;

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

}
