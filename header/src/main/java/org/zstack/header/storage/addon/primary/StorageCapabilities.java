package org.zstack.header.storage.addon.primary;

import org.zstack.header.storage.primary.VolumeSnapshotCapability;
import org.zstack.header.volume.VolumeProtocol;

import java.util.List;

// TODO Merge with PrimaryStorageFeature
public class StorageCapabilities {
    private VolumeSnapshotCapability snapshotCapability;

    private boolean supportMultiSpace;
    private boolean supportCloneFromVolume;
    private boolean supportCloneFromAnotherSpace;
    private boolean supportStorageQos;
    private boolean supportLiveExpandVolume;
    private boolean supportShareableVolume;
    private boolean supportExportVolumeSnapshot;
    public List<String> supportedImageFormats;
    private VolumeProtocol defaultIsoActiveProtocol;
    private VolumeProtocol defaultImageExportProtocol;
    private int minLogicalSectorSize = 512;

    public int getMinLogicalSectorSize() {
        return minLogicalSectorSize;
    }

    public void setMinLogicalSectorSize(int minLogicalSectorSize) {
        this.minLogicalSectorSize = minLogicalSectorSize;
    }

    public boolean isSupportShareableVolume() {
        return supportShareableVolume;
    }

    public void setSupportShareableVolume(boolean supportShareableVolume) {
        this.supportShareableVolume = supportShareableVolume;
    }

    public VolumeProtocol getDefaultIsoActiveProtocol() {
        return defaultIsoActiveProtocol;
    }

    public void setDefaultIsoActiveProtocol(VolumeProtocol defaultIsoActiveProtocol) {
        this.defaultIsoActiveProtocol = defaultIsoActiveProtocol;
    }

    public VolumeProtocol getDefaultImageExportProtocol() {
        return defaultImageExportProtocol;
    }

    public void setDefaultImageExportProtocol(VolumeProtocol defaultImageExportProtocol) {
        this.defaultImageExportProtocol = defaultImageExportProtocol;
    }

    public VolumeSnapshotCapability getSnapshotCapability() {
        return snapshotCapability;
    }

    public void setSnapshotCapability(VolumeSnapshotCapability snapshotCapability) {
        this.snapshotCapability = snapshotCapability;
    }

    public boolean isSupportExportVolumeSnapshot() {
        return supportExportVolumeSnapshot;
    }

    public void setSupportExportVolumeSnapshot(boolean supportExportVolumeSnapshot) {
        this.supportExportVolumeSnapshot = supportExportVolumeSnapshot;
    }

    public boolean isSupportCloneFromVolume() {
        return supportCloneFromVolume;
    }

    public void setSupportCloneFromVolume(boolean supportCloneFromVolume) {
        this.supportCloneFromVolume = supportCloneFromVolume;
    }

    public boolean isSupportCloneFromAnotherSpace() {
        return supportCloneFromAnotherSpace;
    }

    public void setSupportCloneFromAnotherSpace(boolean supportCloneFromAnotherSpace) {
        this.supportCloneFromAnotherSpace = supportCloneFromAnotherSpace;
    }

    public List<String> getSupportedImageFormats() {
        return supportedImageFormats;
    }

    public void setSupportedImageFormats(List<String> supportedImageFormats) {
        this.supportedImageFormats = supportedImageFormats;
    }

    public boolean isSupportStorageQos() {
        return supportStorageQos;
    }

    public void setSupportStorageQos(boolean supportStorageQos) {
        this.supportStorageQos = supportStorageQos;
    }

    public boolean isSupportLiveExpandVolume() {
        return supportLiveExpandVolume;
    }

    public void setSupportLiveExpandVolume(boolean supportLiveExpandVolume) {
        this.supportLiveExpandVolume = supportLiveExpandVolume;
    }

    public boolean isSupportMultiSpace() {
        return supportMultiSpace;
    }

    public void setSupportMultiSpace(boolean supportMultiSpace) {
        this.supportMultiSpace = supportMultiSpace;
    }
}
