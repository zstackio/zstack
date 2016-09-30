package org.zstack.header.storage.primary;

import org.zstack.header.core.ApiTimeout;
import org.zstack.header.vm.APICreateVmInstanceMsg;
import org.zstack.header.vm.VmInstanceSpec.ImageSpec;

@ApiTimeout(apiClasses = {APICreateVmInstanceMsg.class})
public class InstantiateRootVolumeFromTemplateOnPrimaryStorageMsg extends InstantiateVolumeOnPrimaryStorageMsg implements PrimaryStorageMessage {
    private ImageSpec templateSpec;

    public ImageSpec getTemplateSpec() {
        return templateSpec;
    }

    public void setTemplateSpec(ImageSpec templateSpec) {
        this.templateSpec = templateSpec;
    }
}
