package org.zstack.physicalserver;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = PhysicalServerResourceAssignmentVO.class)
@PythonClassInventory
public class PhysicalServerResourceAssignmentInventory implements Serializable {
    private String uuid;
    private String serverUuid;
    private String roleType;
    private String cpuSet;
    private Long memory;
    private String state;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static PhysicalServerResourceAssignmentInventory valueOf(
            PhysicalServerResourceAssignmentVO vo) {
        PhysicalServerResourceAssignmentInventory inventory =
                new PhysicalServerResourceAssignmentInventory();
        inventory.setUuid(vo.getUuid());
        inventory.setServerUuid(vo.getServerUuid());
        inventory.setRoleType(vo.getRoleType());
        inventory.setCpuSet(vo.getCpuSet());
        inventory.setMemory(vo.getMemory());
        inventory.setState(vo.getState().name());
        inventory.setCreateDate(vo.getCreateDate());
        inventory.setLastOpDate(vo.getLastOpDate());
        return inventory;
    }

    public static List<PhysicalServerResourceAssignmentInventory> valueOf(
            Collection<PhysicalServerResourceAssignmentVO> vos) {
        List<PhysicalServerResourceAssignmentInventory> inventories =
                new ArrayList<>(vos.size());
        for (PhysicalServerResourceAssignmentVO vo : vos) {
            inventories.add(valueOf(vo));
        }
        return inventories;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getServerUuid() {
        return serverUuid;
    }

    public void setServerUuid(String serverUuid) {
        this.serverUuid = serverUuid;
    }

    public String getRoleType() {
        return roleType;
    }

    public void setRoleType(String roleType) {
        this.roleType = roleType;
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

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }
}
