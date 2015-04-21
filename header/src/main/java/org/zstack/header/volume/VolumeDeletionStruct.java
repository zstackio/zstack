package org.zstack.header.volume;

/**
 * Created by frank on 4/20/2015.
 */
public class VolumeDeletionStruct extends VolumeInventory {
    private boolean detachBeforeDeleting;

    public VolumeDeletionStruct() {
    }

    public VolumeDeletionStruct(VolumeInventory other) {
        super(other);
    }

    public boolean isDetachBeforeDeleting() {
        return detachBeforeDeleting;
    }

    public void setDetachBeforeDeleting(boolean detachBeforeDeleting) {
        this.detachBeforeDeleting = detachBeforeDeleting;
    }
}
