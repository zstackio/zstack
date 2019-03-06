package org.zstack.sdk;

import org.zstack.sdk.MiniStorageType;

public class MiniStorageInventory extends org.zstack.sdk.PrimaryStorageInventory {

    public java.util.List miniStorageHostRefs;
    public void setMiniStorageHostRefs(java.util.List miniStorageHostRefs) {
        this.miniStorageHostRefs = miniStorageHostRefs;
    }
    public java.util.List getMiniStorageHostRefs() {
        return this.miniStorageHostRefs;
    }

    public MiniStorageType miniStorageType;
    public void setMiniStorageType(MiniStorageType miniStorageType) {
        this.miniStorageType = miniStorageType;
    }
    public MiniStorageType getMiniStorageType() {
        return this.miniStorageType;
    }

    public java.lang.String diskIdentifier;
    public void setDiskIdentifier(java.lang.String diskIdentifier) {
        this.diskIdentifier = diskIdentifier;
    }
    public java.lang.String getDiskIdentifier() {
        return this.diskIdentifier;
    }

}
