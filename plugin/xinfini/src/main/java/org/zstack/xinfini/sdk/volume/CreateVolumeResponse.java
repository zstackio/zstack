package org.zstack.xinfini.sdk.volume;

import org.zstack.xinfini.sdk.BaseResource;
import org.zstack.xinfini.sdk.XInfiniResponse;

/**
 * @ Author : yh.w
 * @ Date   : Created in 18:30 2024/5/29
 */
public class CreateVolumeResponse extends XInfiniResponse {
    private BaseResource.Metadata metadata;
    private VolumeModule.VolumeSpec spec;
    private VolumeModule.VolumeStatus status;

    public BaseResource.Metadata getMetadata() {
        return metadata;
    }

    public void setMetadata(BaseResource.Metadata metadata) {
        this.metadata = metadata;
    }

    public VolumeModule.VolumeSpec getSpec() {
        return spec;
    }

    public void setSpec(VolumeModule.VolumeSpec spec) {
        this.spec = spec;
    }

    public VolumeModule.VolumeStatus getStatus() {
        return status;
    }

    public void setStatus(VolumeModule.VolumeStatus status) {
        this.status = status;
    }

    public VolumeModule toModule() {
        return new VolumeModule(metadata, spec, status);
    }
}
