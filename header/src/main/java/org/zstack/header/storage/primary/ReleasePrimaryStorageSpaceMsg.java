package org.zstack.header.storage.primary;

public class ReleasePrimaryStorageSpaceMsg extends DecreasePrimaryStorageCapacityMsg{
    private String allocatedInstallUrl;

    public String getAllocatedInstallUrl() {
        return allocatedInstallUrl;
    }

    public void setAllocatedInstallUrl(String allocatedInstallUrl) {
        this.allocatedInstallUrl = allocatedInstallUrl;
    }
}
