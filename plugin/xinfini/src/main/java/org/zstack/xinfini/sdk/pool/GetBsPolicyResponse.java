package org.zstack.xinfini.sdk.pool;

import org.zstack.xinfini.sdk.BaseResource;
import org.zstack.xinfini.sdk.XInfiniResponse;

/**
 * @ Author : yh.w
 * @ Date   : Created in 11:51 2024/5/28
 */
public class GetBsPolicyResponse extends XInfiniResponse {
    private BaseResource.Metadata metadata;
    private BsPolicyModule.BsPolicySpec spec;
    private BsPolicyModule.BsPolicyStatus status;

    public BaseResource.Metadata getMetadata() {
        return metadata;
    }

    public void setMetadata(BaseResource.Metadata metadata) {
        this.metadata = metadata;
    }

    public BsPolicyModule.BsPolicySpec getSpec() {
        return spec;
    }

    public void setSpec(BsPolicyModule.BsPolicySpec spec) {
        this.spec = spec;
    }

    public BsPolicyModule.BsPolicyStatus getStatus() {
        return status;
    }

    public void setStatus(BsPolicyModule.BsPolicyStatus status) {
        this.status = status;
    }

    public BsPolicyModule toModule() {
        return new BsPolicyModule(metadata, spec, status);
    }
}
