package org.zstack.header.storage.primary;

public class AllocatePrimaryStorageSpaceMsg extends AllocatePrimaryStorageMsg{
    private String installUrl;

    public String getInstallUrl() {
        return installUrl;
    }

    public void setInstallUrl(String installUrl) {
        this.installUrl = installUrl;
    }
}
