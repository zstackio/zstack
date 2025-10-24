package org.zstack.header.storage.snapshot;

import org.zstack.header.message.OverlayMessage;
import org.zstack.header.volume.VolumeMessage;

public class VolumeSnapshotRevertOverlayVolumeMsg extends OverlayMessage implements VolumeMessage {
    private String volumeUuid;

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }
}
