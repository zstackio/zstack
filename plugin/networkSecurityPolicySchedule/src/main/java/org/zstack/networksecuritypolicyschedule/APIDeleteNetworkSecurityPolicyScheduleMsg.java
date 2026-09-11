package org.zstack.networksecuritypolicyschedule;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@Action(category = NetworkSecurityPolicyScheduleConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/network-security-policy-schedules/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteNetworkSecurityPolicyScheduleEvent.class
)
public class APIDeleteNetworkSecurityPolicyScheduleMsg extends APIDeleteMessage {
    @APIParam(
            resourceType = NetworkSecurityPolicyScheduleVO.class,
            successIfResourceNotExisting = true
    )
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static APIDeleteNetworkSecurityPolicyScheduleMsg __example__() {
        APIDeleteNetworkSecurityPolicyScheduleMsg msg =
                new APIDeleteNetworkSecurityPolicyScheduleMsg();
        msg.setUuid(uuid());
        return msg;
    }
}
