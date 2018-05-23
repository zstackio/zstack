package org.zstack.header.identity.role.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.PolicyVO;
import org.zstack.header.identity.role.RoleVO;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

import static org.codehaus.groovy.runtime.InvokerHelper.asList;

@RestRequest(path = "/identities/policies/{policyUuid}/roles/{roleUuid}", method = HttpMethod.DELETE, responseClass = APIDetachPolicyFromRoleEvent.class)
public class APIDetachPolicyFromRoleMsg extends APIMessage implements RoleMessage {
    @APIParam
    private String roleUuid;
    @APIParam
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

    public static APIDetachPolicyFromRoleMsg __example__() {
        APIDetachPolicyFromRoleMsg msg = new APIDetachPolicyFromRoleMsg();
        msg.setRoleUuid(uuid());
        msg.setPolicyUuid(uuid());

        return msg;
    }
}
