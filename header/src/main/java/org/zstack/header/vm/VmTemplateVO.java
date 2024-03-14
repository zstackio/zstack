package org.zstack.header.vm;

import org.zstack.header.vo.*;
import org.zstack.header.zone.ZoneEO;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.sql.Timestamp;

@Entity
@Table
@BaseResource
public class VmTemplateVO extends ResourceVO implements ToInventory {
    @Column
    private String name;

    @Column
    @ForeignKey(parentEntityClass = VmInstanceEO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String vmInstanceUuid;

    @Column
    @ForeignKey(parentEntityClass = ZoneEO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String zoneUuid;
    @Column
    private String originalType;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

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

    public String getZoneUuid() {
        return zoneUuid;
    }

    public void setZoneUuid(String zoneUuid) {
        this.zoneUuid = zoneUuid;
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
