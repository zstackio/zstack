package org.zstack.xinfini.sdk.vhost;

import org.zstack.xinfini.sdk.BaseResource;
import org.zstack.xinfini.sdk.XInfiniResponse;
import org.zstack.xinfini.sdk.volume.VolumeModule;

/**
 * @ Author : yh.w
 * @ Date   : Created in 18:30 2024/5/29
 */
public class CreateBdcBdevResponse extends XInfiniResponse {
    private BaseResource.Metadata metadata;
    private BdcBdevModule.BdcBdevSpec spec;
    private BdcBdevModule.BdcBdevStatus status;

    public BaseResource.Metadata getMetadata() {
        return metadata;
    }

    public void setMetadata(BaseResource.Metadata metadata) {
        this.metadata = metadata;
    }

    public BdcBdevModule.BdcBdevSpec getSpec() {
        return spec;
    }

    public void setSpec(BdcBdevModule.BdcBdevSpec spec) {
        this.spec = spec;
    }

    public BdcBdevModule.BdcBdevStatus getStatus() {
        return status;
    }

    public void setStatus(BdcBdevModule.BdcBdevStatus status) {
        this.status = status;
    }

    public BdcBdevModule toModule() {
        return new BdcBdevModule(metadata, spec, status);
    }
}
