package org.zstack.header.storage.primary;

public class AllocatePrimaryStorageSpaceMsg extends AllocatePrimaryStorageMsg {
    private String requiredAllocatedInstallUrl;

    public String getRequiredAllocatedInstallUrl() {
        return requiredAllocatedInstallUrl;
    }

    public void setRequiredAllocatedInstallUrl(String requiredAllocatedInstallUrl) {
        this.requiredAllocatedInstallUrl = requiredAllocatedInstallUrl;
    }
}
