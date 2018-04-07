package org.zstack.header.identity.role.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.rest.RestRequest;

import java.util.List;

@RestRequest(path = "/identities/roles/{uuid}/policy-statements", method = HttpMethod.DELETE, responseClass = APIRemovePolicyStatementsFromRoleEvent.class)
public class APIRemovePolicyStatementsFromRoleMsg extends APIMessage implements RoleMessage {
    private String uuid;
    private List<String> policyStatementUuids;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public List<String> getPolicyStatementUuids() {
        return policyStatementUuids;
    }

    public void setPolicyStatementUuids(List<String> policyStatementUuids) {
        this.policyStatementUuids = policyStatementUuids;
    }

    @Override
    public String getRoleUuid() {
        return uuid;
    }
}
