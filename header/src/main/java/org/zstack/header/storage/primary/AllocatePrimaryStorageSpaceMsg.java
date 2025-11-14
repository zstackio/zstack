package org.zstack.header.storage.primary;

public class AllocatePrimaryStorageSpaceMsg extends AllocatePrimaryStorageMsg {
    /**
     * Indicates whether to force allocate primary storage space even if the primary storage
     * is marked as full. Because some volume snapshot instantiated before allocated
     */
    private boolean force;
    private String requiredInstallUri;

    public boolean isForce() {
        return force;
    }

    public void setForce(boolean force) {
        this.force = force;
    }

    public String getRequiredInstallUri() {
        return requiredInstallUri;
    }

    public void setRequiredInstallUri(String requiredInstallUrl) {
        this.requiredInstallUri = requiredInstallUrl;
    }
}
