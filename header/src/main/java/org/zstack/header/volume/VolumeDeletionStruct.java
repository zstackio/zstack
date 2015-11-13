package org.zstack.header.volume;

/**
 * Created by frank on 4/20/2015.
 */
public class VolumeDeletionStruct extends VolumeInventory {
    private boolean detachBeforeDeleting;
    private String deletionPolicy;

    public VolumeDeletionStruct() {
    }

    public String getDeletionPolicy() {
        return deletionPolicy;
    }

    public void setDeletionPolicy(String deletionPolicy) {
        this.deletionPolicy = deletionPolicy;
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
