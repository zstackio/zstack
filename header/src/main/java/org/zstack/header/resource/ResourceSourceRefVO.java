package org.zstack.header.resource;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.sql.Timestamp;

@Entity
@Table(name = "ResourceSourceRefVO", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"resourceUuid", "resourceType", "sourceType", "syncType"})
})
public class ResourceSourceRefVO {
    @Id
    @Column(nullable = false, length = 32)
    private String uuid;
    @Column(nullable = false, length = 32)
    private String resourceUuid;
    @Column(nullable = false, length = 64)
    private String resourceType;
    @Column(nullable = false, length = 64)
    private String sourceType;
    @Column(length = 128)
    private String sourceName;
    @Column(length = 32)
    private String externalUuid;
    @Column(length = 255)
    private String externalType;
    @Column(nullable = false, length = 64)
    private String syncType;
    @Column(nullable = false)
    private Timestamp createDate;
    @Column(nullable = false)
    private Timestamp lastOpDate;

    @PrePersist
    private void prePersist() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        if (createDate == null) {
            createDate = now;
        }
        if (lastOpDate == null) {
            lastOpDate = now;
        }
    }

    @PreUpdate
    private void preUpdate() {
        lastOpDate = new Timestamp(System.currentTimeMillis());
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public String getExternalUuid() {
        return externalUuid;
    }

    public void setExternalUuid(String externalUuid) {
        this.externalUuid = externalUuid;
    }

    public String getExternalType() {
        return externalType;
    }

    public void setExternalType(String externalType) {
        this.externalType = externalType;
    }

    public String getSyncType() {
        return syncType;
    }

    public void setSyncType(String syncType) {
        this.syncType = syncType;
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
