package org.zstack.header.volume;

import org.zstack.header.core.ApiTimeout;
import org.zstack.header.vm.APICreateVmInstanceMsg;
import org.zstack.header.vm.VmInstanceSpec.ImageSpec;

/**
 * Created by xing5 on 2016/8/22.
 */
@ApiTimeout(apiClasses = {APICreateVmInstanceMsg.class})
public class InstantiateRootVolumeMsg extends InstantiateVolumeMsg implements VolumeMessage {
    private ImageSpec templateSpec;

    public ImageSpec getTemplateSpec() {
        return templateSpec;
    }

    public void setTemplateSpec(ImageSpec templateSpec) {
        this.templateSpec = templateSpec;
    }
}
