package org.zstack.header.volume;

import org.zstack.header.message.DeletionMessage;

/**
 */
public class VolumeDeletionMsg extends DeletionMessage implements VolumeMessage {
    private String volumeUuid;

    @Override
    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }
}
