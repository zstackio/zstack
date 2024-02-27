package org.zstack.header.storage.addon.primary;

import org.zstack.header.storage.primary.VolumeSnapshotCapability;

import java.util.List;

public class StorageCapabilities {
    private VolumeSnapshotCapability snapshotCapability;

    private boolean supportCloneFromVolume;

    private boolean supportStorageQos;

    private boolean supportLiveExpandVolume;
    public List<String> supportedImageFormats;

    public VolumeSnapshotCapability getSnapshotCapability() {
        return snapshotCapability;
    }

    public void setSnapshotCapability(VolumeSnapshotCapability snapshotCapability) {
        this.snapshotCapability = snapshotCapability;
    }

    public boolean isSupportCloneFromVolume() {
        return supportCloneFromVolume;
    }

    public void setSupportCloneFromVolume(boolean supportCloneFromVolume) {
        this.supportCloneFromVolume = supportCloneFromVolume;
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
}
