package org.zstack.network.service.eip;

import org.zstack.header.identity.Action;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.network.service.vip.VipVO;

/**
 */
@Action(category = EipConstant.ACTION_CATEGORY, names = {"read"})
public class APIGetEipAttachableVmNicsMsg extends APISyncCallMessage {
    @APIParam(required = false, resourceType = EipVO.class)
    private String eipUuid;
    @APIParam(required = false, resourceType = VipVO.class)
    private String vipUuid;

    public String getEipUuid() {
        return eipUuid;
    }

    public void setEipUuid(String eipUuid) {
        this.eipUuid = eipUuid;
    }

    public String getVipUuid() {
        return vipUuid;
    }

    public void setVipUuid(String vipUuid) {
        this.vipUuid = vipUuid;
    }
}
