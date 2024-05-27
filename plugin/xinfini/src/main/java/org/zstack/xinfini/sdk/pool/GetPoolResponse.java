package org.zstack.xinfini.sdk.pool;

import org.zstack.xinfini.sdk.BaseResource;
import org.zstack.xinfini.sdk.XInfiniResponse;

/**
 * @ Author : yh.w
 * @ Date   : Created in 11:51 2024/5/28
 */
public class GetPoolResponse extends XInfiniResponse {
    private BaseResource.Metadata metadata;
    private PoolModule.PoolSpec spec;
    private PoolModule.PoolStatus status;

    public BaseResource.Metadata getMetadata() {
        return metadata;
    }

    public void setMetadata(BaseResource.Metadata metadata) {
        this.metadata = metadata;
    }

    public PoolModule.PoolSpec getSpec() {
        return spec;
    }

    public void setSpec(PoolModule.PoolSpec spec) {
        this.spec = spec;
    }

    public PoolModule.PoolStatus getStatus() {
        return status;
    }

    public void setStatus(PoolModule.PoolStatus status) {
        this.status = status;
    }

    public PoolModule toModule() {
        return new PoolModule(metadata, spec, status);
    }
}
