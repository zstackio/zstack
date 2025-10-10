package org.zstack.header.identity;


import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table
public class ResourceResponsibleVO {

    @Column
    @Id
    private String uuid;

    @Column
    private String resourceUuid;

    @Column
    private String responsibleType;

    @Column
    private String responsibleUuid;

    @Column
    private Timestamp lastOpDate;

    @Column
    private Timestamp createDate;


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

    public String getResponsibleType() {
        return responsibleType;
    }

    public void setResponsibleType(String responsibleType) {
        this.responsibleType = responsibleType;
    }

    public String getResponsibleUuid() {
        return responsibleUuid;
    }

    public void setResponsibleUuid(String responsibleUuid) {
        this.responsibleUuid = responsibleUuid;
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }
}
