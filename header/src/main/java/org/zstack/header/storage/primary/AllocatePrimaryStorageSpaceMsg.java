package org.zstack.header.storage.primary;

public class AllocatePrimaryStorageSpaceMsg extends AllocatePrimaryStorageMsg {
    private String requireAllocatedInstallUrl;

    public String getRequireAllocatedInstallUrl() {
        return requireAllocatedInstallUrl;
    }

    public void setRequireAllocatedInstallUrl(String requireAllocatedInstallUrl) {
        this.requireAllocatedInstallUrl = requireAllocatedInstallUrl;
    }
}
