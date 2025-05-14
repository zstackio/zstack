package org.zstack.network.service.portforwarding;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 */
@Action(category = PortForwardingConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/port-forwarding/{uuid}/actions",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APIChangePortForwardingRuleEvent.class
)
public class APIChangePortForwardingRuleMsg extends APIMessage {
    @APIParam(resourceType = PortForwardingRuleVO.class, checkAccount = true, operationTarget = true)
    private String uuid;
    @APIParam(required = false)
    private String allowedCidr;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getAllowedCidr() {
        return allowedCidr;
    }

    public void setAllowedCidr(String allowedCidr) {
        this.allowedCidr = allowedCidr;
    }

    public static APIChangePortForwardingRuleMsg __example__() {
        APIChangePortForwardingRuleMsg msg = new APIChangePortForwardingRuleMsg();
        msg.setUuid(uuid());
        msg.setAllowedCidr("192.168.1.0/24,192.168.2.100/32");
        return msg;
    }
}
