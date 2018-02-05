package org.zstack.storage.primary.nfs;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.primary.PrimaryStorageMessage;

/**
 * Created by GuoYi on 10/19/17.
 */
public class NfsToNfsMigrateVolumeMsg extends NeedReplyMessage implements PrimaryStorageMessage {
    private String hostUuid;
    private String srcVolumeFolderPath;
    private String dstVolumeFolderPath;
    private String primaryStorageUuid;

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getSrcVolumeFolderPath() {
        return srcVolumeFolderPath;
    }

    public void setSrcVolumeFolderPath(String srcVolumeFolderPath) {
        this.srcVolumeFolderPath = srcVolumeFolderPath;
    }

    public String getDstVolumeFolderPath() {
        return dstVolumeFolderPath;
    }

    public void setDstVolumeFolderPath(String dstVolumeFolderPath) {
        this.dstVolumeFolderPath = dstVolumeFolderPath;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    @Override
    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }
}
