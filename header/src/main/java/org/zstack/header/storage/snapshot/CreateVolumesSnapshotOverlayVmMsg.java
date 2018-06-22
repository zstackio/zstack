package org.zstack.header.storage.snapshot;

import org.zstack.header.core.ApiTimeout;
import org.zstack.header.message.OverlayMessage;
import org.zstack.header.vm.VmInstanceMessage;
import org.zstack.header.volume.APICreateVolumeSnapshotMsg;

/**
 * Create by weiwang at 2018/6/11
 */
@ApiTimeout(apiClasses = {APICreateVolumeSnapshotMsg.class})
public class CreateVolumesSnapshotOverlayVmMsg extends OverlayMessage implements VmInstanceMessage {
    private String vmInstanceUuid;

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
}
