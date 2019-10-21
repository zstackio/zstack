package org.zstack.sdk;

import org.zstack.sdk.SessionStatus;
import org.zstack.sdk.SessionType;

public class PortMirrorSessionInventory  {

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
    }

    public java.lang.String name;
    public void setName(java.lang.String name) {
        this.name = name;
    }
    public java.lang.String getName() {
        return this.name;
    }

    public java.lang.String description;
    public void setDescription(java.lang.String description) {
        this.description = description;
    }
    public java.lang.String getDescription() {
        return this.description;
    }

    public SessionStatus status;
    public void setStatus(SessionStatus status) {
        this.status = status;
    }
    public SessionStatus getStatus() {
        return this.status;
    }

    public java.lang.Long internalId;
    public void setInternalId(java.lang.Long internalId) {
        this.internalId = internalId;
    }
    public java.lang.Long getInternalId() {
        return this.internalId;
    }

    public java.lang.String srcEndPoint;
    public void setSrcEndPoint(java.lang.String srcEndPoint) {
        this.srcEndPoint = srcEndPoint;
    }
    public java.lang.String getSrcEndPoint() {
        return this.srcEndPoint;
    }

    public SessionType type;
    public void setType(SessionType type) {
        this.type = type;
    }
    public SessionType getType() {
        return this.type;
    }

    public java.lang.String dstEndPoint;
    public void setDstEndPoint(java.lang.String dstEndPoint) {
        this.dstEndPoint = dstEndPoint;
    }
    public java.lang.String getDstEndPoint() {
        return this.dstEndPoint;
    }

    public java.lang.String portMirrorUuid;
    public void setPortMirrorUuid(java.lang.String portMirrorUuid) {
        this.portMirrorUuid = portMirrorUuid;
    }
    public java.lang.String getPortMirrorUuid() {
        return this.portMirrorUuid;
    }

    public java.sql.Timestamp createDate;
    public void setCreateDate(java.sql.Timestamp createDate) {
        this.createDate = createDate;
    }
    public java.sql.Timestamp getCreateDate() {
        return this.createDate;
    }

    public java.sql.Timestamp lastOpDate;
    public void setLastOpDate(java.sql.Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }
    public java.sql.Timestamp getLastOpDate() {
        return this.lastOpDate;
    }

}
