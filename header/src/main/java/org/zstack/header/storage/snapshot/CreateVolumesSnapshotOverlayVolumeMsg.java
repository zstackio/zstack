package org.zstack.header.storage.snapshot;

import org.zstack.header.core.ApiTimeout;
import org.zstack.header.message.OverlayMessage;
import org.zstack.header.vm.VmInstanceMessage;
import org.zstack.header.volume.APICreateVolumeSnapshotMsg;
import org.zstack.header.volume.VolumeMessage;

/**
 * Create by weiwang at 2018/6/11
 */
@ApiTimeout(apiClasses = {APICreateVolumeSnapshotMsg.class})
public class CreateVolumesSnapshotOverlayVolumeMsg extends OverlayMessage implements VolumeMessage {
    private String volumeUuid;

    @Override
    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }
}
