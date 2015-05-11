package org.zstack.header.volume;

import org.zstack.header.message.DeletionMessage;

/**
 */
public class VolumeDeletionMsg extends DeletionMessage implements VolumeMessage {
    private String volumeUuid;
    private boolean detachBeforeDeleting;

    public boolean isDetachBeforeDeleting() {
        return detachBeforeDeleting;
    }

    public void setDetachBeforeDeleting(boolean detachBeforeDeleting) {
        this.detachBeforeDeleting = detachBeforeDeleting;
    }

    @Override
    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }
}
