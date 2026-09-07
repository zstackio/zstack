package org.zstack.header.storage.primary;

import org.zstack.header.message.MessageReply;

/**
 */
public class ReInitRootVolumeFromTemplateOnPrimaryStorageReply extends MessageReply {
    private String newVolumeInstallPath;
    private Long imageCacheId;

    public Long getImageCacheId() {
        return imageCacheId;
    }

    public void setImageCacheId(Long imageCacheId) {
        this.imageCacheId = imageCacheId;
    }

    public String getNewVolumeInstallPath() {
        return newVolumeInstallPath;
    }

    public void setNewVolumeInstallPath(String newVolumeInstallPath) {
        this.newVolumeInstallPath = newVolumeInstallPath;
    }
}
