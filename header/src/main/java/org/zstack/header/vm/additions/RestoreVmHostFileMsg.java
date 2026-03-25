package org.zstack.header.vm.additions;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.vm.VmInstanceMessage;

public class RestoreVmHostFileMsg extends NeedReplyMessage implements VmInstanceMessage {
    private String vmInstanceUuid;
    private String snapshotGroupUuid;

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
}