package org.zstack.header.storage.primary;

import org.zstack.header.log.NoLogging;

public class AllocatePrimaryStorageSpaceMsg extends AllocatePrimaryStorageMsg {
    @NoLogging
    private String requiredInstallUri;

    public String getRequiredInstallUri() {
        return requiredInstallUri;
    }

    public void setRequiredInstallUri(String requiredInstallUrl) {
        this.requiredInstallUri = requiredInstallUrl;
    }
}
