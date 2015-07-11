package org.zstack.network.service.portforwarding;

import org.zstack.header.identity.Action;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;

/**
 */
@Action(category = PortForwardingConstant.ACTION_CATEGORY)
public class APIGetPortForwardingAttachableVmNicsMsg extends APISyncCallMessage {
    @APIParam(resourceType = PortForwardingRuleVO.class)
    private String ruleUuid;

    public String getRuleUuid() {
        return ruleUuid;
    }

    public void setRuleUuid(String portForwardingRuleUuid) {
        this.ruleUuid = portForwardingRuleUuid;
    }
}
