package org.zstack.header.vm;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@PythonClassInventory
@Inventory(mappingVOClass = VmTemplateVO.class)
public class VmTemplateInventory implements Serializable {
    private String uuid;
    private String name;
    private String vmInstanceUuid;
    private String originalType;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public VmTemplateInventory() {
    }

    public VmTemplateInventory(VmTemplateInventory other) {
        this.uuid = other.getUuid();
        this.name = other.getName();
        this.vmInstanceUuid = other.getVmInstanceUuid();
        this.originalType = other.getOriginalType();
        this.createDate = other.getCreateDate();
        this.lastOpDate = other.getLastOpDate();
    }

    public static VmTemplateInventory valueOf(VmTemplateVO vo) {
        VmTemplateInventory inventory = new VmTemplateInventory();
        inventory.setUuid(vo.getUuid());
        inventory.setName(vo.getName());
        inventory.setVmInstanceUuid(vo.getVmInstanceUuid());
        inventory.setOriginalType(vo.getOriginalType());
        inventory.setCreateDate(vo.getCreateDate());
        inventory.setLastOpDate(vo.getLastOpDate());
        return inventory;
    }

    public static List<VmTemplateInventory> valueOf(Collection<VmTemplateVO> vos) {
        return vos.stream().map(VmTemplateInventory::valueOf).collect(Collectors.toList());
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public String getOriginalType() {
        return originalType;
    }

    public void setOriginalType(String originalType) {
        this.originalType = originalType;
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
