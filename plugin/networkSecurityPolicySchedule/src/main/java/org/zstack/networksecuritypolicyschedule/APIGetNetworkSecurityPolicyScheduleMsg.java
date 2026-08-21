package org.zstack.networksecuritypolicyschedule;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vo.ResourceVO;

@Action(category = NetworkSecurityPolicyScheduleConstant.ACTION_CATEGORY, names = {"read"})
@RestRequest(
        path = "/network-security-policy-schedules",
        method = HttpMethod.GET,
        responseClass = APIGetNetworkSecurityPolicyScheduleReply.class
)
public class APIGetNetworkSecurityPolicyScheduleMsg extends APISyncCallMessage {
    @APIParam(resourceType = ResourceVO.class, checkAccount = true)
    private String resourceUuid;

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public static APIGetNetworkSecurityPolicyScheduleMsg __example__() {
        APIGetNetworkSecurityPolicyScheduleMsg msg =
                new APIGetNetworkSecurityPolicyScheduleMsg();
        msg.setResourceUuid(uuid());
        return msg;
    }
}
