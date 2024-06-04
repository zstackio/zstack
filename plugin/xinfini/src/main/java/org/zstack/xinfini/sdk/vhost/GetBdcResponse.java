package org.zstack.xinfini.sdk.vhost;

import org.zstack.xinfini.sdk.BaseResource;
import org.zstack.xinfini.sdk.XInfiniResponse;
import org.zstack.xinfini.sdk.node.NodeModule;

/**
 * @ Author : yh.w
 * @ Date   : Created in 11:51 2024/5/28
 */
public class GetBdcResponse extends XInfiniResponse {
    private BaseResource.Metadata metadata;
    private BdcModule.BdcSpec spec;
    private BdcModule.BdcStatus status;

    public BaseResource.Metadata getMetadata() {
        return metadata;
    }

    public void setMetadata(BaseResource.Metadata metadata) {
        this.metadata = metadata;
    }

    public BdcModule.BdcSpec getSpec() {
        return spec;
    }

    public void setSpec(BdcModule.BdcSpec spec) {
        this.spec = spec;
    }

    public BdcModule.BdcStatus getStatus() {
        return status;
    }

    public void setStatus(BdcModule.BdcStatus status) {
        this.status = status;
    }

    public BdcModule toModule() {
        return new BdcModule(metadata, spec, status);
    }
}
