package org.zstack.sdk;



public class KeyProviderRelatedVmInventory  {

    public java.lang.String providerUuid;
    public void setProviderUuid(java.lang.String providerUuid) {
        this.providerUuid = providerUuid;
    }
    public java.lang.String getProviderUuid() {
        return this.providerUuid;
    }

    public java.lang.String vmInstanceUuid;
    public void setVmInstanceUuid(java.lang.String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
    public java.lang.String getVmInstanceUuid() {
        return this.vmInstanceUuid;
    }

    public java.lang.String name;
    public void setName(java.lang.String name) {
        this.name = name;
    }
    public java.lang.String getName() {
        return this.name;
    }

    public java.lang.String state;
    public void setState(java.lang.String state) {
        this.state = state;
    }
    public java.lang.String getState() {
        return this.state;
    }

    public boolean tpmUsesProvider;
    public void setTpmUsesProvider(boolean tpmUsesProvider) {
        this.tpmUsesProvider = tpmUsesProvider;
    }
    public boolean getTpmUsesProvider() {
        return this.tpmUsesProvider;
    }

    public java.util.List snapshotGroupUuids;
    public void setSnapshotGroupUuids(java.util.List snapshotGroupUuids) {
        this.snapshotGroupUuids = snapshotGroupUuids;
    }
    public java.util.List getSnapshotGroupUuids() {
        return this.snapshotGroupUuids;
    }

    public java.util.List backupUuids;
    public void setBackupUuids(java.util.List backupUuids) {
        this.backupUuids = backupUuids;
    }
    public java.util.List getBackupUuids() {
        return this.backupUuids;
    }

    public java.sql.Timestamp lastOpDate;
    public void setLastOpDate(java.sql.Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }
    public java.sql.Timestamp getLastOpDate() {
        return this.lastOpDate;
    }

}
