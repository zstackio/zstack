package org.zstack.physicalserver;

import org.zstack.header.physicalserver.ManagedServiceResourceUsage;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PhysicalServerManagedServiceInventory implements Serializable {
    private String roleType;
    private String serviceName;
    private boolean restartable;
    private boolean restartRequired;
    private String state;
    private String cpuSet;
    private Long cpuTime;
    private Long memory;
    private Long memoryLimit;

    public static PhysicalServerManagedServiceInventory valueOf(ManagedServiceResourceUsage usage) {
        PhysicalServerManagedServiceInventory inventory = new PhysicalServerManagedServiceInventory();
        inventory.setRoleType(usage.getRoleType());
        inventory.setServiceName(usage.getServiceName());
        inventory.setRestartable(usage.isRestartable());
        inventory.setRestartRequired(usage.isRestartRequired());
        inventory.setState(usage.getState());
        inventory.setCpuSet(usage.getCpuSet());
        inventory.setCpuTime(usage.getCpuTime());
        inventory.setMemory(usage.getMemory());
        inventory.setMemoryLimit(usage.getMemoryLimit());
        return inventory;
    }

    public static List<PhysicalServerManagedServiceInventory> valueOf(Collection<ManagedServiceResourceUsage> usages) {
        List<PhysicalServerManagedServiceInventory> result = new ArrayList<>();
        for (ManagedServiceResourceUsage usage : usages) {
            result.add(valueOf(usage));
        }
        return result;
    }

    public String getRoleType() {
        return roleType;
    }

    public void setRoleType(String roleType) {
        this.roleType = roleType;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public boolean isRestartable() {
        return restartable;
    }

    public void setRestartable(boolean restartable) {
        this.restartable = restartable;
    }

    public boolean isRestartRequired() {
        return restartRequired;
    }

    public void setRestartRequired(boolean restartRequired) {
        this.restartRequired = restartRequired;
    }

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
