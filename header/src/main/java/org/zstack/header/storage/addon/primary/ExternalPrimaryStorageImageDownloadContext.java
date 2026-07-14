package org.zstack.header.storage.addon.primary;

import org.zstack.header.image.ImageInventory;
import org.zstack.header.storage.addon.RemoteTarget;
import org.zstack.header.volume.VolumeStats;

public class ExternalPrimaryStorageImageDownloadContext {
    private String primaryStorageUuid;
    private String primaryStorageType;
    private ImageInventory image;
    private CreateVolumeSpec spec;
    private String targetResourceType;
    private VolumeStats volume;
    private RemoteTarget remoteTarget;

    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public String getPrimaryStorageType() {
        return primaryStorageType;
    }

    public void setPrimaryStorageType(String primaryStorageType) {
        this.primaryStorageType = primaryStorageType;
    }

    public ImageInventory getImage() {
        return image;
    }

    public void setImage(ImageInventory image) {
        this.image = image;
    }

    public CreateVolumeSpec getSpec() {
        return spec;
    }

    public void setSpec(CreateVolumeSpec spec) {
        this.spec = spec;
    }

    public String getTargetResourceType() {
        return targetResourceType;
    }

    public void setTargetResourceType(String targetResourceType) {
        this.targetResourceType = targetResourceType;
    }

    public VolumeStats getVolume() {
        return volume;
    }

    public void setVolume(VolumeStats volume) {
        this.volume = volume;
    }

    public RemoteTarget getRemoteTarget() {
        return remoteTarget;
    }

    public void setRemoteTarget(RemoteTarget remoteTarget) {
        this.remoteTarget = remoteTarget;
    }

}
