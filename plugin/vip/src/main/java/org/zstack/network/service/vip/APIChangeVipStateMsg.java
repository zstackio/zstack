package org.zstack.network.service.vip;

import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;

/**
 */
@Action(category = VipConstant.ACTION_CATEGORY)
public class APIChangeVipStateMsg extends APIMessage {
    @APIParam(resourceType = VipVO.class, checkAccount = true, operationTarget = true)
    private String uuid;
    @APIParam(validValues = {"enable", "disable"})
    private String stateEvent;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String vipUuid) {
        this.uuid = vipUuid;
    }

    public String getStateEvent() {
        return stateEvent;
    }

    public void setStateEvent(String stateEvent) {
        this.stateEvent = stateEvent;
    }
}
