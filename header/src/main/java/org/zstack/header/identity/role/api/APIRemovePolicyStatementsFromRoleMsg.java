package org.zstack.header.identity.role.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.role.RolePolicyStatementVO;
import org.zstack.header.identity.role.RoleVO;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

import java.util.Collections;
import java.util.List;

@RestRequest(path = "/identities/roles/{uuid}/policy-statements", method = HttpMethod.DELETE, responseClass = APIRemovePolicyStatementsFromRoleEvent.class)
public class APIRemovePolicyStatementsFromRoleMsg extends APIMessage implements RoleMessage {
    @APIParam(resourceType = RoleVO.class)
    private String uuid;
    @APIParam(resourceType = RolePolicyStatementVO.class)
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

    public static APIRemovePolicyStatementsFromRoleMsg __example__() {
        APIRemovePolicyStatementsFromRoleMsg msg = new APIRemovePolicyStatementsFromRoleMsg();
        msg.setUuid(uuid());
        msg.setPolicyStatementUuids(Collections.singletonList(uuid()));

        return msg;
    }
}
