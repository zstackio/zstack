package org.zstack.xinfini.sdk.iscsi;

import org.zstack.xinfini.sdk.BaseResource;
import org.zstack.xinfini.sdk.XInfiniResponse;

/**
 * @ Author : yh.w
 * @ Date   : Created in 11:51 2024/5/28
 */
public class GetVolumeClientGroupMappingResponse extends XInfiniResponse {
    private BaseResource.Metadata metadata;
    private VolumeClientGroupMappingModule.VolumeClientGroupMappingSpec spec;
    private VolumeClientGroupMappingModule.VolumeClientGroupMappingStatus status;

    public BaseResource.Metadata getMetadata() {
        return metadata;
    }

    public void setMetadata(BaseResource.Metadata metadata) {
        this.metadata = metadata;
    }

    public VolumeClientGroupMappingModule.VolumeClientGroupMappingSpec getSpec() {
        return spec;
    }

    public void setSpec(VolumeClientGroupMappingModule.VolumeClientGroupMappingSpec spec) {
        this.spec = spec;
    }

    public VolumeClientGroupMappingModule.VolumeClientGroupMappingStatus getStatus() {
        return status;
    }

    public void setStatus(VolumeClientGroupMappingModule.VolumeClientGroupMappingStatus status) {
        this.status = status;
    }

    public VolumeClientGroupMappingModule toModule() {
        return new VolumeClientGroupMappingModule(metadata, spec, status);
    }
}
