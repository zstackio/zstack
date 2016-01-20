package org.zstack.header.vm;

import org.zstack.header.identity.Action;
import org.zstack.header.image.ImageVO;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;

/**
 * Created by frank on 10/17/2015.
 */
@Action(category = VmInstanceConstant.ACTION_CATEGORY)
public class APIAttachIsoToVmInstanceMsg extends APIMessage implements VmInstanceMessage {
    @APIParam(resourceType = VmInstanceVO.class, checkAccount = true, operationTarget = true)
    private String vmInstanceUuid;
    @APIParam(resourceType = ImageVO.class, checkAccount = true)
    private String isoUuid;

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public String getIsoUuid() {
        return isoUuid;
    }

    public void setIsoUuid(String isoUuid) {
        this.isoUuid = isoUuid;
    }
}
