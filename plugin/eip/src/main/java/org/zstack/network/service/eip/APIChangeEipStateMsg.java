package org.zstack.network.service.eip;

import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;

/**
 */
@Action(category = EipConstant.ACTION_CATEGORY)
public class APIChangeEipStateMsg extends APIMessage implements EipMessage {
    @APIParam(resourceType = EipVO.class, checkAccount = true, operationTarget = true)
    private String uuid;
    @APIParam(validValues = {"enable", "disable"})
    private String stateEvent;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getStateEvent() {
        return stateEvent;
    }

    public void setStateEvent(String stateEvent) {
        this.stateEvent = stateEvent;
    }

    @Override
    public String getEipUuid() {
        return uuid;
    }
}
