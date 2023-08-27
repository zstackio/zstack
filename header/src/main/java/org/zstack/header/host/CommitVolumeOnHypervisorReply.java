package org.zstack.header.host;

import org.zstack.header.message.MessageReply;

public class CommitVolumeOnHypervisorReply extends MessageReply {
    private String newVolumeInstallPath;
    private long size;

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getNewVolumeInstallPath() {
        return newVolumeInstallPath;
    }

    public void setNewVolumeInstallPath(String newVolumeInstallPath) {
        this.newVolumeInstallPath = newVolumeInstallPath;
    }
}
