package org.zstack.header.storage.primary;

public class AllocatePrimaryStorageSpaceMsg extends AllocatePrimaryStorageMsg {
    private String requiredInstallUrl;

    public String getRequiredInstallUrl() {
        return requiredInstallUrl;
    }

    public void setRequiredInstallUrl(String requiredInstallUrl) {
        this.requiredInstallUrl = requiredInstallUrl;
    }
}
