package org.zstack.header.storage.primary;

import org.zstack.header.message.NeedReplyMessage;

import java.util.List;

public class DeleteVolumeChainOnPrimaryStorageMsg extends NeedReplyMessage implements PrimaryStorageMessage {
    private String primaryStorageUuid;

    private String hostUuid;

    private List<String> installPaths;

    private String volumeFormat;

    @Override
    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setInstallPaths(List<String> installPaths) {
        this.installPaths = installPaths;
    }

    public List<String> getInstallPaths() {
        return installPaths;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getVolumeFormat() {
        return volumeFormat;
    }

    public void setVolumeFormat(String volumeFormat) {
        this.volumeFormat = volumeFormat;
    }
}
