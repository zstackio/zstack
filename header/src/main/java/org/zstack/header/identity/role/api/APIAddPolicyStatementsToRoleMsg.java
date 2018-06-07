package org.zstack.header.identity.role.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.PolicyStatement;
import org.zstack.header.identity.StatementEffect;
import org.zstack.header.identity.role.RoleVO;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@RestRequest(path = "/identities/roles/{uuid}/policy-statements", method = HttpMethod.POST,
        parameterName = "params", responseClass = APIAddPolicyStatementsToRoleEvent.class)
public class APIAddPolicyStatementsToRoleMsg extends APIMessage implements RoleMessage {
    @APIParam(resourceType = RoleVO.class)
    private String uuid;
    @APIParam(nonempty = true)
    private List<PolicyStatement> statements;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public List<PolicyStatement> getStatements() {
        return statements;
    }

    public void setStatements(List<PolicyStatement> statements) {
        this.statements = statements;
    }

    @Override
    public String getRoleUuid() {
        return uuid;
    }

    public static APIAddPolicyStatementsToRoleMsg __example__() {
        APIAddPolicyStatementsToRoleMsg msg = new APIAddPolicyStatementsToRoleMsg();

        msg.setUuid(uuid());

        PolicyStatement state = new PolicyStatement();
        state.setName("state-1");
        state.setEffect(StatementEffect.Allow);
        state.setActions(asList("accpet"));

        msg.setStatements(asList(state));

        return msg;
    }
}
