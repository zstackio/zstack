package org.zstack.header.storage.addon.primary;

import org.zstack.header.storage.primary.PrimaryStorageEO;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ToInventory;

import jakarta.persistence.*;
import java.sql.Timestamp;

@Entity
@Table
public class ExternalPrimaryStorageSpaceVO implements ToInventory {
    @Column
    @Id
    private String uuid;

    @Column
    @ForeignKey(parentEntityClass = PrimaryStorageEO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String primaryStorageUuid;

    @Column
    private String locationUrl;

    @Column
    private String type;

    @Column
    private String name;

    @Column
    private long availableCapacity;

    @Column
    private long totalCapacity;

    @Column
    private long availablePhysicalCapacity;

    @Column
    private long totalPhysicalCapacity;

    @Column
    private Timestamp createDate;
    @Column
    private Timestamp lastOpDate;


    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public String getLocationUrl() {
        return locationUrl;
    }

    public void setLocationUrl(String locationUrl) {
        this.locationUrl = locationUrl;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getAvailableCapacity() {
        return availableCapacity;
    }

    public void setAvailableCapacity(long availableCapacity) {
        this.availableCapacity = availableCapacity;
    }

    public long getTotalCapacity() {
        return totalCapacity;
    }

    public void setTotalCapacity(long totalCapacity) {
        this.totalCapacity = totalCapacity;
    }

    public long getAvailablePhysicalCapacity() {
        return availablePhysicalCapacity;
    }

    public void setAvailablePhysicalCapacity(long availablePhysicalCapacity) {
        this.availablePhysicalCapacity = availablePhysicalCapacity;
    }

    public long getTotalPhysicalCapacity() {
        return totalPhysicalCapacity;
    }

    public void setTotalPhysicalCapacity(long totalPhysicalCapacity) {
        this.totalPhysicalCapacity = totalPhysicalCapacity;
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
