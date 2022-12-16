package org.zstack.sdk.disasterrecovery;

import org.zstack.sdk.disasterrecovery.MirrorCdpTaskMode;
import org.zstack.sdk.disasterrecovery.MirrorCdpTaskStatus;
import org.zstack.sdk.disasterrecovery.MirrorCdpTaskState;
import org.zstack.sdk.CdpTaskInventory;

public class MirrorCdpTaskInventory  {

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

    public java.lang.String peerExternalManagementNodeUuid;
    public void setPeerExternalManagementNodeUuid(java.lang.String peerExternalManagementNodeUuid) {
        this.peerExternalManagementNodeUuid = peerExternalManagementNodeUuid;
    }
    public java.lang.String getPeerExternalManagementNodeUuid() {
        return this.peerExternalManagementNodeUuid;
    }

    public java.lang.String peerCdpTaskUuid;
    public void setPeerCdpTaskUuid(java.lang.String peerCdpTaskUuid) {
        this.peerCdpTaskUuid = peerCdpTaskUuid;
    }
    public java.lang.String getPeerCdpTaskUuid() {
        return this.peerCdpTaskUuid;
    }

    public MirrorCdpTaskMode mode;
    public void setMode(MirrorCdpTaskMode mode) {
        this.mode = mode;
    }
    public MirrorCdpTaskMode getMode() {
        return this.mode;
    }

    public java.lang.String peerHostName;
    public void setPeerHostName(java.lang.String peerHostName) {
        this.peerHostName = peerHostName;
    }
    public java.lang.String getPeerHostName() {
        return this.peerHostName;
    }

    public java.lang.String peerUserName;
    public void setPeerUserName(java.lang.String peerUserName) {
        this.peerUserName = peerUserName;
    }
    public java.lang.String getPeerUserName() {
        return this.peerUserName;
    }

    public java.lang.String peerPassword;
    public void setPeerPassword(java.lang.String peerPassword) {
        this.peerPassword = peerPassword;
    }
    public java.lang.String getPeerPassword() {
        return this.peerPassword;
    }

    public java.lang.String mirrorResourceUuid;
    public void setMirrorResourceUuid(java.lang.String mirrorResourceUuid) {
        this.mirrorResourceUuid = mirrorResourceUuid;
    }
    public java.lang.String getMirrorResourceUuid() {
        return this.mirrorResourceUuid;
    }

    public java.lang.String mirrorResourceType;
    public void setMirrorResourceType(java.lang.String mirrorResourceType) {
        this.mirrorResourceType = mirrorResourceType;
    }
    public java.lang.String getMirrorResourceType() {
        return this.mirrorResourceType;
    }

    public MirrorCdpTaskStatus status;
    public void setStatus(MirrorCdpTaskStatus status) {
        this.status = status;
    }
    public MirrorCdpTaskStatus getStatus() {
        return this.status;
    }

    public MirrorCdpTaskState state;
    public void setState(MirrorCdpTaskState state) {
        this.state = state;
    }
    public MirrorCdpTaskState getState() {
        return this.state;
    }

    public CdpTaskInventory cdpTask;
    public void setCdpTask(CdpTaskInventory cdpTask) {
        this.cdpTask = cdpTask;
    }
    public CdpTaskInventory getCdpTask() {
        return this.cdpTask;
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
