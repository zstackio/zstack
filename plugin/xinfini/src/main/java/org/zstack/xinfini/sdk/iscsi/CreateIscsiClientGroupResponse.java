package org.zstack.xinfini.sdk.iscsi;

import org.zstack.xinfini.sdk.BaseResource;
import org.zstack.xinfini.sdk.XInfiniResponse;

/**
 * @ Author : yh.w
 * @ Date   : Created in 19:28 2024/5/29
 */
public class CreateIscsiClientGroupResponse extends XInfiniResponse {
    private BaseResource.Metadata metadata;
    private IscsiClientGroupModule.IscsiClientGroupSpec spec;
    private IscsiClientGroupModule.IscsiClientGroupStatus status;

    public BaseResource.Metadata getMetadata() {
        return metadata;
    }

    public void setMetadata(BaseResource.Metadata metadata) {
        this.metadata = metadata;
    }

    public IscsiClientGroupModule.IscsiClientGroupSpec getSpec() {
        return spec;
    }

    public void setSpec(IscsiClientGroupModule.IscsiClientGroupSpec spec) {
        this.spec = spec;
    }

    public IscsiClientGroupModule.IscsiClientGroupStatus getStatus() {
        return status;
    }

    public void setStatus(IscsiClientGroupModule.IscsiClientGroupStatus status) {
        this.status = status;
    }

    public IscsiClientGroupModule toModule() {
        return new IscsiClientGroupModule(metadata, spec, status);
    }
}
