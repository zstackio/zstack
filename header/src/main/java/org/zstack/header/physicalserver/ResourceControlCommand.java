package org.zstack.header.physicalserver;

import java.util.ArrayList;
import java.util.List;

public class ResourceControlCommand {
    private String roleType;
    private String isolationMode;
    private String operation;
    private String cpuSet;
    private Long memory;
    private String sliceName;
    private boolean includeAuxiliaryServices;
    private List<ResourceConsumerHandle> handles = new ArrayList<>();

    public String getRoleType() {
        return roleType;
    }

    public void setRoleType(String roleType) {
        this.roleType = roleType;
    }

    public String getIsolationMode() {
        return isolationMode;
    }

    public void setIsolationMode(String isolationMode) {
        this.isolationMode = isolationMode;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
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

    public String getSliceName() {
        return sliceName;
    }

    public void setSliceName(String sliceName) {
        this.sliceName = sliceName;
    }

    public boolean isIncludeAuxiliaryServices() {
        return includeAuxiliaryServices;
    }

    public void setIncludeAuxiliaryServices(boolean includeAuxiliaryServices) {
        this.includeAuxiliaryServices = includeAuxiliaryServices;
    }

    public List<ResourceConsumerHandle> getHandles() {
        return handles;
    }

    public void setHandles(List<ResourceConsumerHandle> handles) {
        this.handles = handles;
    }
}
