package org.zstack.header.physicalserver;

import java.util.ArrayList;
import java.util.List;

public class ResourceControlResponse {
    private String state;
    private String cpuSet;
    private Long memory;
    private Integer coveredServiceCount;
    private Integer expectedServiceCount;
    private List<ResourceControlResult> results = new ArrayList<>();

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

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

    public Integer getCoveredServiceCount() {
        return coveredServiceCount;
    }

    public void setCoveredServiceCount(Integer coveredServiceCount) {
        this.coveredServiceCount = coveredServiceCount;
    }

    public Integer getExpectedServiceCount() {
        return expectedServiceCount;
    }

    public void setExpectedServiceCount(Integer expectedServiceCount) {
        this.expectedServiceCount = expectedServiceCount;
    }

    public List<ResourceControlResult> getResults() {
        return results;
    }

    public void setResults(List<ResourceControlResult> results) {
        this.results = results;
    }
}
