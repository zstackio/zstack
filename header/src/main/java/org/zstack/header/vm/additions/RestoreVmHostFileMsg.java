package org.zstack.header.vm.additions;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.vm.VmInstanceMessage;

public class RestoreVmHostFileMsg extends NeedReplyMessage implements VmInstanceMessage {
    private String vmInstanceUuid;
    private String snapshotGroupUuid;
    private String backupResourceUuid;
    private boolean skipDeletingNvRam;
    private String syncReason;

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public String getSnapshotGroupUuid() {
        return snapshotGroupUuid;
    }

    public void setSnapshotGroupUuid(String snapshotGroupUuid) {
        this.snapshotGroupUuid = snapshotGroupUuid;
    }

    public String getBackupResourceUuid() {
        return backupResourceUuid != null ? backupResourceUuid : snapshotGroupUuid;
    }

    public void setBackupResourceUuid(String backupResourceUuid) {
        this.backupResourceUuid = backupResourceUuid;
    }

    public boolean isSkipDeletingNvRam() {
        return skipDeletingNvRam;
    }

    public void setSkipDeletingNvRam(boolean skipDeletingNvRam) {
        this.skipDeletingNvRam = skipDeletingNvRam;
    }

    public String getSyncReason() {
        return syncReason;
    }

    public void setSyncReason(String syncReason) {
        this.syncReason = syncReason;
    }
}