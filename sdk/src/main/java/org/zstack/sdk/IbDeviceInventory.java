package org.zstack.sdk;

import org.zstack.sdk.IbAllocateStatus;
import org.zstack.sdk.IbBindingSource;
import org.zstack.sdk.IbCollectionStatus;

public class IbDeviceInventory extends org.zstack.sdk.PciDeviceInventory {

    public IbAllocateStatus allocateStatus;
    public void setAllocateStatus(IbAllocateStatus allocateStatus) {
        this.allocateStatus = allocateStatus;
    }
    public IbAllocateStatus getAllocateStatus() {
        return this.allocateStatus;
    }

    public IbBindingSource bindingSource;
    public void setBindingSource(IbBindingSource bindingSource) {
        this.bindingSource = bindingSource;
    }
    public IbBindingSource getBindingSource() {
        return this.bindingSource;
    }

    public IbCollectionStatus collectionStatus;
    public void setCollectionStatus(IbCollectionStatus collectionStatus) {
        this.collectionStatus = collectionStatus;
    }
    public IbCollectionStatus getCollectionStatus() {
        return this.collectionStatus;
    }

    public java.sql.Timestamp lastCollectedAt;
    public void setLastCollectedAt(java.sql.Timestamp lastCollectedAt) {
        this.lastCollectedAt = lastCollectedAt;
    }
    public java.sql.Timestamp getLastCollectedAt() {
        return this.lastCollectedAt;
    }

    public java.lang.String sharingMode;
    public void setSharingMode(java.lang.String sharingMode) {
        this.sharingMode = sharingMode;
    }
    public java.lang.String getSharingMode() {
        return this.sharingMode;
    }

    public java.util.List unavailableReasons;
    public void setUnavailableReasons(java.util.List unavailableReasons) {
        this.unavailableReasons = unavailableReasons;
    }
    public java.util.List getUnavailableReasons() {
        return this.unavailableReasons;
    }

}
