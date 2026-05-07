package org.zstack.storage.ceph.primary;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.primary.PrimaryStorageMessage;

/**
 * Exports bits of a ceph/rbd volume on this primary storage to a path on a KVM host (ssh), using cephagent sftp upload.
 */
public class SftpExportCephVolumeToKvmHostMsg extends NeedReplyMessage implements PrimaryStorageMessage {
    private String primaryStorageUuid;
    private String cephVolumeInstallPath;
    private String dstHostUuid;
    private String dstAbsolutePathOnHost;

    @Override
    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public String getCephVolumeInstallPath() {
        return cephVolumeInstallPath;
    }

    public void setCephVolumeInstallPath(String cephVolumeInstallPath) {
        this.cephVolumeInstallPath = cephVolumeInstallPath;
    }

    public String getDstHostUuid() {
        return dstHostUuid;
    }

    public void setDstHostUuid(String dstHostUuid) {
        this.dstHostUuid = dstHostUuid;
    }

    public String getDstAbsolutePathOnHost() {
        return dstAbsolutePathOnHost;
    }

    public void setDstAbsolutePathOnHost(String dstAbsolutePathOnHost) {
        this.dstAbsolutePathOnHost = dstAbsolutePathOnHost;
    }
}
