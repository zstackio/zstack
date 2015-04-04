package org.zstack.network.service.portforwarding;

import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;

/**
 */
public class APIChangePortForwardingRuleStateMsg extends APIMessage {
    @APIParam(resourceType = PortForwardingRuleVO.class)
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
}
