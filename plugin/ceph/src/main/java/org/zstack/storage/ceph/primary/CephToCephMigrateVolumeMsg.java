package org.zstack.storage.ceph.primary;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.primary.PrimaryStorageMessage;

/**
 * Created by GuoYi on 10/19/17.
 */
public class CephToCephMigrateVolumeMsg extends NeedReplyMessage implements PrimaryStorageMessage {
    private String volumeUuid;
    private long volumeSize;
    private String srcInstallPath;
    private String dstInstallPath;
    private String dstMonHostname;
    private String dstMonSshUsername;
    private String dstMonSshPassword;
    private int dstMonSshPort;
    private String primaryStorageuuid;

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public long getVolumeSize() {
        return volumeSize;
    }

    public void setVolumeSize(long volumeSize) {
        this.volumeSize = volumeSize;
    }

    public String getSrcInstallPath() {
        return srcInstallPath;
    }

    public void setSrcInstallPath(String srcInstallPath) {
        this.srcInstallPath = srcInstallPath;
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
        return primaryStorageuuid;
    }

    public void setPrimaryStorageuuid(String primaryStorageuuid) {
        this.primaryStorageuuid = primaryStorageuuid;
    }
}
