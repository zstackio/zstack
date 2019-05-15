package org.zstack.header.host;

import org.zstack.header.message.MessageReply;

/**
 * Created by mingjian.deng on 2019/4/12.
 */
public class CheckSnapshotOnHypervisorReply extends MessageReply {
    private Long size;
    private boolean completed;
    private String volumeInstallPath;
    private String snapshotInstallPath;

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public String getVolumeInstallPath() {
        return volumeInstallPath;
    }

    public void setVolumeInstallPath(String volumeInstallPath) {
        this.volumeInstallPath = volumeInstallPath;
    }

    public String getSnapshotInstallPath() {
        return snapshotInstallPath;
    }

    public void setSnapshotInstallPath(String snapshotInstallPath) {
        this.snapshotInstallPath = snapshotInstallPath;
    }
}
