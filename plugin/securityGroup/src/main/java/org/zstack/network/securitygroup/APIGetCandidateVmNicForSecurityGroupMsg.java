package org.zstack.network.securitygroup;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 */
@Action(category = SecurityGroupConstant.ACTION_CATEGORY, names = {"read"})
@RestRequest(
        path = "/security-groups/{securityGroupUuid}/vm-instances/candidate-nics",
        method = HttpMethod.GET,
        responseClass = APIGetCandidateVmNicForSecurityGroupReply.class
)
public class APIGetCandidateVmNicForSecurityGroupMsg extends APISyncCallMessage {
    @APIParam(resourceType = SecurityGroupVO.class, checkAccount = true)
    private String securityGroupUuid;

    public String getSecurityGroupUuid() {
        return securityGroupUuid;
    }

    public void setSecurityGroupUuid(String securityGroupUuid) {
        this.securityGroupUuid = securityGroupUuid;
    }
}
