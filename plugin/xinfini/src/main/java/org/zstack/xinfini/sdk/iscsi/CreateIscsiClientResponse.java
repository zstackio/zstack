package org.zstack.xinfini.sdk.iscsi;

import org.zstack.xinfini.sdk.BaseResource;
import org.zstack.xinfini.sdk.XInfiniResponse;

/**
 * @ Author : yh.w
 * @ Date   : Created in 19:28 2024/5/29
 */
public class CreateIscsiClientResponse extends XInfiniResponse {
    private BaseResource.Metadata metadata;
    private IscsiClientModule.IscsiClientSpec spec;
    private IscsiClientModule.IscsiClientStatus status;

    public BaseResource.Metadata getMetadata() {
        return metadata;
    }

    public void setMetadata(BaseResource.Metadata metadata) {
        this.metadata = metadata;
    }

    public IscsiClientModule.IscsiClientSpec getSpec() {
        return spec;
    }

    public void setSpec(IscsiClientModule.IscsiClientSpec spec) {
        this.spec = spec;
    }

    public IscsiClientModule.IscsiClientStatus getStatus() {
        return status;
    }

    public void setStatus(IscsiClientModule.IscsiClientStatus status) {
        this.status = status;
    }

    public IscsiClientModule toModule() {
        return new IscsiClientModule(metadata, spec, status);
    }
}
