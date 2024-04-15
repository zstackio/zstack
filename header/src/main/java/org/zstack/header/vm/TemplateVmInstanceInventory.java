package org.zstack.header.vm;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.Queryable;
import org.zstack.header.search.Inventory;

import javax.persistence.JoinColumn;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@PythonClassInventory
@Inventory(mappingVOClass = TemplateVmInstanceVO.class)
public class TemplateVmInstanceInventory implements Serializable {
    private String uuid;

    @Queryable(mappingClass = VmInstanceInventory.class,
            joinColumn = @JoinColumn(name = "uuid", referencedColumnName = "name"))
    private String name;

    @Queryable(mappingClass = VmInstanceInventory.class,
            joinColumn = @JoinColumn(name = "uuid", referencedColumnName = "zoneUuid"))
    private String zoneUuid;

    private Timestamp createDate;

    private Timestamp lastOpDate;

    public TemplateVmInstanceInventory() {
    }

    public TemplateVmInstanceInventory(TemplateVmInstanceInventory other) {
        this.uuid = other.getUuid();
        this.name = other.getName();
        this.zoneUuid = other.getZoneUuid();
        this.createDate = other.getCreateDate();
        this.lastOpDate = other.getLastOpDate();
    }

    public static TemplateVmInstanceInventory valueOf(TemplateVmInstanceVO vo)                                                                                                                                                                                                                                                                                                                                                                                                                                                    {
        TemplateVmInstanceInventory inventory = new TemplateVmInstanceInventory();
        inventory.setUuid(vo.getUuid());
        if (vo.getVm() != null) {
            inventory.setName(vo.getVm().getName());
            inventory.setZoneUuid(vo.getVm().getZoneUuid());
        }
        inventory.setCreateDate(vo.getCreateDate());
        inventory.setLastOpDate(vo.getLastOpDate());
        return inventory;
    }

    public static List<TemplateVmInstanceInventory> valueOf(Collection<TemplateVmInstanceVO> vos) {
        return vos.stream().map(TemplateVmInstanceInventory::valueOf).collect(Collectors.toList());
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

    public String getZoneUuid() {
        return zoneUuid;
    }

    public void setZoneUuid(String zoneUuid) {
        this.zoneUuid = zoneUuid;
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
