package org.zstack.sdk.disasterrecovery;



public class DisasterRecoveryVmTemplateInventory extends org.zstack.sdk.VmTemplateInventory {

    public java.lang.String mirrorCdpTaskUuid;
    public void setMirrorCdpTaskUuid(java.lang.String mirrorCdpTaskUuid) {
        this.mirrorCdpTaskUuid = mirrorCdpTaskUuid;
    }
    public java.lang.String getMirrorCdpTaskUuid() {
        return this.mirrorCdpTaskUuid;
    }

    public java.lang.String templateType;
    public void setTemplateType(java.lang.String templateType) {
        this.templateType = templateType;
    }
    public java.lang.String getTemplateType() {
        return this.templateType;
    }

    public java.lang.String failbackDestination;
    public void setFailbackDestination(java.lang.String failbackDestination) {
        this.failbackDestination = failbackDestination;
    }
    public java.lang.String getFailbackDestination() {
        return this.failbackDestination;
    }

    public boolean useExistingVolume;
    public void setUseExistingVolume(boolean useExistingVolume) {
        this.useExistingVolume = useExistingVolume;
    }
    public boolean getUseExistingVolume() {
        return this.useExistingVolume;
    }

    public long groupId;
    public void setGroupId(long groupId) {
        this.groupId = groupId;
    }
    public long getGroupId() {
        return this.groupId;
    }

    public java.lang.String originVmInstanceUuid;
    public void setOriginVmInstanceUuid(java.lang.String originVmInstanceUuid) {
        this.originVmInstanceUuid = originVmInstanceUuid;
    }
    public java.lang.String getOriginVmInstanceUuid() {
        return this.originVmInstanceUuid;
    }

    public java.lang.String externalManagementTagUuids;
    public void setExternalManagementTagUuids(java.lang.String externalManagementTagUuids) {
        this.externalManagementTagUuids = externalManagementTagUuids;
    }
    public java.lang.String getExternalManagementTagUuids() {
        return this.externalManagementTagUuids;
    }

}
