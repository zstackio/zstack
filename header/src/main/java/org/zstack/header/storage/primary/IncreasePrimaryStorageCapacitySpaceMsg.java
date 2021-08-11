package org.zstack.header.storage.primary;

public class IncreasePrimaryStorageCapacitySpaceMsg extends IncreasePrimaryStorageCapacityMsg {
    private String installUrl;

    public String getInstallUrl() {
        return installUrl;
    }

    public void setInstallUrl(String installUrl) {
        this.installUrl = installUrl;
    }
}
