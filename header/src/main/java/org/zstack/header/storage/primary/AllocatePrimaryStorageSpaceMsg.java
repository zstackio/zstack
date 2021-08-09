package org.zstack.header.storage.primary;

public class AllocatePrimaryStorageSpaceMsg extends AllocatePrimaryStorageMsg {
    private String InstallUrl;

    public String getInstallUrl() {
        return InstallUrl;
    }

    public void setInstallUrl(String installUrl) {
        InstallUrl = installUrl;
    }
}
