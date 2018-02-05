package org.zstack.storage.ceph.primary;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.primary.PrimaryStorageMessage;

/**
 * Created by GuoYi on 10/19/17.
 */
public class CephToCephMigrateVolumeSnapshotMsg extends NeedReplyMessage implements PrimaryStorageMessage {
    private String parentUuid;
    private String snapshotUuid;
    private long snapshotSize;
    private String srcSnapshotPath;
    private String dstInstallPath;
    private String dstMonHostname;
    private String dstMonSshUsername;
    private String dstMonSshPassword;
    private int dstMonSshPort;
    private String primaryStorageUuid;

    public String getParentUuid() {
        return parentUuid;
    }

    public void setParentUuid(String parentUuid) {
        this.parentUuid = parentUuid;
    }

    public String getSnapshotUuid() {
        return snapshotUuid;
    }

    public void setSnapshotUuid(String snapshotUuid) {
        this.snapshotUuid = snapshotUuid;
    }

    public long getSnapshotSize() {
        return snapshotSize;
    }

    public void setSnapshotSize(long snapshotSize) {
        this.snapshotSize = snapshotSize;
    }

    public String getSrcSnapshotPath() {
        return srcSnapshotPath;
    }

    public void setSrcSnapshotPath(String srcSnapshotPath) {
        this.srcSnapshotPath = srcSnapshotPath;
    }

    public String getDstInstallPath() {
        return dstInstallPath;
    }

    public void setDstInstallPath(String dstInstallPath) {
        this.dstInstallPath = dstInstallPath;
    }

    public String getDstMonHostname() {
        return dstMonHostname;
    }

    public void setDstMonHostname(String dstMonHostname) {
        this.dstMonHostname = dstMonHostname;
    }

    public String getDstMonSshUsername() {
        return dstMonSshUsername;
    }

    public void setDstMonSshUsername(String dstMonSshUsername) {
        this.dstMonSshUsername = dstMonSshUsername;
    }

    public String getDstMonSshPassword() {
        return dstMonSshPassword;
    }

    public void setDstMonSshPassword(String dstMonSshPassword) {
        this.dstMonSshPassword = dstMonSshPassword;
    }

    public int getDstMonSshPort() {
        return dstMonSshPort;
    }

    public void setDstMonSshPort(int dstMonSshPort) {
        this.dstMonSshPort = dstMonSshPort;
    }

    @Override
    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }
}
