package org.zstack.header.storage.primary;

import org.zstack.header.core.ApiTimeout;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.volume.APICreateDataVolumeFromVolumeSnapshotMsg;

@ApiTimeout(apiClasses = {APICreateDataVolumeFromVolumeSnapshotMsg.class})
public class DeleteBitsOnPrimaryStorageMsg extends NeedReplyMessage implements PrimaryStorageMessage {
    private String primaryStorageUuid;
    private String installPath;
    private String hypervisorType;
    private boolean folder;
    private String bitsUuid;
    private String bitsType;

    public String getBitsUuid() {
        return bitsUuid;
    }

    public void setBitsUuid(String bitsUuid) {
        this.bitsUuid = bitsUuid;
    }

    public String getBitsType() {
        return bitsType;
    }

    public void setBitsType(String bitsType) {
        this.bitsType = bitsType;
    }

    public boolean isFolder() {
        return folder;
    }

    public void setFolder(boolean folder) {
        this.folder = folder;
    }

    @Override
    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public String getInstallPath() {
        return installPath;
    }

    public String getHypervisorType() {
        return hypervisorType;
    }

    public void setHypervisorType(String hypervisorType) {
        this.hypervisorType = hypervisorType;
    }

    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }
}
