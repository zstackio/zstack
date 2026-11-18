package org.zstack.storage.zbs;

public class ZbsCgroupResourceUsage {
    private String cgroupName;
    private String cpuSet;
    private Long cpuTime;
    private Long memory;
    private Long memoryLimit;

    public String getCgroupName() {
        return cgroupName;
    }

    public void setCgroupName(String cgroupName) {
        this.cgroupName = cgroupName;
    }

    public String getCpuSet() {
        return cpuSet;
    }

    public void setCpuSet(String cpuSet) {
        this.cpuSet = cpuSet;
    }

    public Long getCpuTime() {
        return cpuTime;
    }

    public void setCpuTime(Long cpuTime) {
        this.cpuTime = cpuTime;
    }

    public Long getMemory() {
        return memory;
    }

    public void setMemory(Long memory) {
        this.memory = memory;
    }

    public Long getMemoryLimit() {
        return memoryLimit;
    }

    public void setMemoryLimit(Long memoryLimit) {
        this.memoryLimit = memoryLimit;
    }
}
