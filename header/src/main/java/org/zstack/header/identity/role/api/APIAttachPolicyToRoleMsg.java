package org.zstack.header.identity.role.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.PolicyVO;
import org.zstack.header.identity.role.RoleVO;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/identities/policies/{policyUuid}/roles/{roleUuid}",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APIAttachPolicyToRoleEvent.class
)
public class APIAttachPolicyToRoleMsg extends APIMessage implements RoleMessage {
    @APIParam(resourceType = RoleVO.class, checkAccount = true, operationTarget = true)
    private String roleUuid;
    @APIParam(resourceType = PolicyVO.class, checkAccount = true, operationTarget = true)
    private String policyUuid;

    public String getRoleUuid() {
        return roleUuid;
    }

    public void setRoleUuid(String roleUuid) {
        this.roleUuid = roleUuid;
    }

    public String getPolicyUuid() {
        return policyUuid;
    }

    public void setPolicyUuid(String policyUuid) {
        this.policyUuid = policyUuid;
    }

    public static APIAttachPolicyToRoleMsg __example__() {
        APIAttachPolicyToRoleMsg msg = new APIAttachPolicyToRoleMsg();

        msg.setRoleUuid(uuid());
        msg.setPolicyUuid(uuid());

        return msg;
    }
}
