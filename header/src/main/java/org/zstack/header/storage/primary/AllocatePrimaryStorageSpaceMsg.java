package org.zstack.header.storage.primary;

public class AllocatePrimaryStorageSpaceMsg extends AllocatePrimaryStorageMsg{
    private String RequireInstallUrl;

    public String getRequireInstallUrl() {
        return RequireInstallUrl;
    }

    public void setRequireInstallUrl(String requireInstallUrl) {
        RequireInstallUrl = requireInstallUrl;
    }
}
