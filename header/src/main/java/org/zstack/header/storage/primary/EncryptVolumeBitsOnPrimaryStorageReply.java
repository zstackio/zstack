package org.zstack.header.storage.primary;

import org.zstack.header.message.MessageReply;

public class EncryptVolumeBitsOnPrimaryStorageReply extends MessageReply {
    private String installPath;
    private Long actualSize;

    public String getInstallPath() {
        return installPath;
    }

    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }

    public Long getActualSize() {
        return actualSize;
    }

    public void setActualSize(Long actualSize) {
        this.actualSize = actualSize;
    }
}
