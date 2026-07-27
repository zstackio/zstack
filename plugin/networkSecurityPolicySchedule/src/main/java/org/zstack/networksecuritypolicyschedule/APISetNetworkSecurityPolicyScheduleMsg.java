package org.zstack.networksecuritypolicyschedule;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vo.ResourceVO;

@Action(category = NetworkSecurityPolicyScheduleConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/network-security-policy-schedules/actions",
        method = HttpMethod.PUT,
        responseClass = APISetNetworkSecurityPolicyScheduleEvent.class,
        isAction = true
)
public class APISetNetworkSecurityPolicyScheduleMsg extends APIMessage {
    @APIParam(required = false, resourceType = NetworkSecurityPolicyScheduleVO.class)
    private String scheduleUuid;

    @APIParam(validValues = {
            NetworkSecurityPolicyScheduleConstant.SECURITY_GROUP_RESOURCE_TYPE,
            NetworkSecurityPolicyScheduleConstant.VPC_FIREWALL_RULE_SET_RESOURCE_TYPE
    })
    private String resourceType;

    @APIParam(resourceType = ResourceVO.class, checkAccount = true, operationTarget = true)
    private String resourceUuid;

    public String getScheduleUuid() {
        return scheduleUuid;
    }

    public void setScheduleUuid(String scheduleUuid) {
        this.scheduleUuid = scheduleUuid;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }
}
