package org.zstack.header.storage.primary;

/**
 * Created by frank on 6/9/2015.
 */
public class VolumeSnapshotCapability {
    public boolean isSupportCreateOnHypervisor() {
        return supportCreateOnHypervisor;
    }

    public void setSupportCreateOnHypervisor(boolean supportCreateOnHypervisor) {
        this.supportCreateOnHypervisor = supportCreateOnHypervisor;
    }

    public boolean isSupportLazyDelete() {
        return supportLazyDelete;
    }

    public void setSupportLazyDelete(boolean supportLazyDelete) {
        this.supportLazyDelete = supportLazyDelete;
    }

    public static enum VolumeSnapshotArrangementType {
        CHAIN,
        INDIVIDUAL
    }

    private boolean support;

    /***
     * Whether the primary storage supports creating volume snapshots by hypervisor.
     */
    private boolean supportCreateOnHypervisor;


    /***
     * Whether the primary storage supports lazy delete for volume snapshots.
     * even if snapshot is not ready for delete, it can be auto-deleted in storage backend.
     * so client can delete snapshot immediately without waiting for reference cleaned.
     */
    private boolean supportLazyDelete;

    private VolumeSnapshotArrangementType arrangementType;

    public boolean isSupport() {
        return support;
    }

    public void setSupport(boolean support) {
        this.support = support;
    }

    public VolumeSnapshotArrangementType getArrangementType() {
        return arrangementType;
    }

    public void setArrangementType(VolumeSnapshotArrangementType arrangementType) {
        this.arrangementType = arrangementType;
    }
}
