package org.zstack.xinfini.sdk.volume;

import org.zstack.xinfini.sdk.BaseResource;
import org.zstack.xinfini.sdk.XInfiniResponse;

/**
 * @ Author : yh.w
 * @ Date   : Created in 18:30 2024/5/29
 */
public class CreateVolumeSnapshotResponse extends XInfiniResponse {
    private BaseResource.Metadata metadata;
    private VolumeSnapshotModule.VolumeSnapshotSpec spec;
    private VolumeSnapshotModule.VolumeSnapshotStatus status;

    public BaseResource.Metadata getMetadata() {
        return metadata;
    }

    public void setMetadata(BaseResource.Metadata metadata) {
        this.metadata = metadata;
    }

    public VolumeSnapshotModule.VolumeSnapshotSpec getSpec() {
        return spec;
    }

    public void setSpec(VolumeSnapshotModule.VolumeSnapshotSpec spec) {
        this.spec = spec;
    }

    public VolumeSnapshotModule.VolumeSnapshotStatus getStatus() {
        return status;
    }

    public void setStatus(VolumeSnapshotModule.VolumeSnapshotStatus status) {
        this.status = status;
    }

    public VolumeSnapshotModule toModule() {
        return new VolumeSnapshotModule(metadata, spec, status);
    }
}
