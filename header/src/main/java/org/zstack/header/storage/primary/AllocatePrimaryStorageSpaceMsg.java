package org.zstack.header.storage.primary;

import org.zstack.header.volume.VolumeInventory;

public class AllocatePrimaryStorageSpaceMsg extends AllocatePrimaryStorageMsg {
    private String requireAllocatedInstallUrl;

    public String getRequireAllocatedInstallUrl() {
        return requireAllocatedInstallUrl;
    }

    public void setRequireAllocatedInstallUrl(String requireAllocatedInstallUrl) {
        this.requireAllocatedInstallUrl = requireAllocatedInstallUrl;
    }
}
