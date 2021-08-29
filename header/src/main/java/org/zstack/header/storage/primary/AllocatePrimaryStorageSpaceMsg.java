package org.zstack.header.storage.primary;

public class AllocatePrimaryStorageSpaceMsg extends AllocatePrimaryStorageMsg {
    /***
     * requiredInstallUrl : file://xxx;volume://xxx
     * 1. nfs smp sblk aliyun : file is not empty, volume is empty
     * 2. local ceph mini     : file is empty, volume is not empty
     */
    private String requiredInstallUrl;

    public String getRequiredInstallUrl() {
        return requiredInstallUrl;
    }

    public void setRequiredInstallUrl(String requiredInstallUrl) {
        this.requiredInstallUrl = requiredInstallUrl;
    }
}
