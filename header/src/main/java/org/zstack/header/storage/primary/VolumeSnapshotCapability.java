package org.zstack.header.storage.primary;

import java.util.function.Function;

/**
 * Created by frank on 6/9/2015.
 */
public class VolumeSnapshotCapability {
    public static enum VolumeSnapshotArrangementType {
        CHAIN,
        INDIVIDUAL
    }
    
    public static enum VolumeSnapshotPlacementType {
        INTERNAL,
        EXTERNAL,
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

    private VolumeSnapshotPlacementType placementType;
    
    /***
     * If volume snapshot is inner snapshot on volume, it must be set.
     * A regex match volume install path from inner volume snapshot install path.
     * such as pool/vol from pool/vol@snapshot can be extracted by regex ^[^@]+
     */
    private String volumePathFromInternalSnapshotRegex;

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

    public VolumeSnapshotPlacementType getPlacementType() {
        return placementType;
    }

    public void setPlacementType(VolumeSnapshotPlacementType placementType) {
        this.placementType = placementType;
    }

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

    @Deprecated
    public String getVolumePathFromInternalSnapshotRegex() {
        return volumePathFromInternalSnapshotRegex;
    }

    @Deprecated
    public void setVolumePathFromInternalSnapshotRegex(String volumePathFromInternalSnapshotRegex) {
        this.volumePathFromInternalSnapshotRegex = volumePathFromInternalSnapshotRegex;
    }
}
