package org.zstack.header.identity.role.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.PolicyStatement;
import org.zstack.header.identity.PolicyVO;
import org.zstack.header.identity.StatementEffect;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@RestRequest(path = "/identities/roles", method = HttpMethod.POST, responseClass = APICreateRoleEvent.class, parameterName = "params")
public class APICreateRoleMsg extends APICreateMessage {
    @APIParam(maxLength = 255)
    private String name;
    @APIParam(maxLength = 2048, required = false)
    private String description;
    private List<PolicyStatement> statements;
    @APIParam(resourceType = PolicyVO.class, required = false)
    private List<String> policyUuids;
    @APIParam(required = false, maxLength = 255, emptyString = false)
    private String identity;
    @APIParam(required = false, maxLength = 32, emptyString = false)
    private String rootUuid;

    public List<String> getPolicyUuids() {
        return policyUuids;
    }

    public void setPolicyUuids(List<String> policyUuids) {
        this.policyUuids = policyUuids;
    }

    public List<PolicyStatement> getStatements() {
        return statements;
    }

    public void setStatements(List<PolicyStatement> statements) {
        this.statements = statements;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIdentity() {
        return identity;
    }

    public void setIdentity(String identity) {
        this.identity = identity;
    }

    public String getRootUuid() {
        return rootUuid;
    }

    public void setRootUuid(String rootUuid) {
        this.rootUuid = rootUuid;
    }

    public static APICreateRoleMsg __example__() {
        APICreateRoleMsg msg = new APICreateRoleMsg();
        msg.setName("role-1");
        msg.setPolicyUuids(asList(uuid()));

        PolicyStatement statement = new PolicyStatement();
        statement.setEffect(StatementEffect.Allow);
        statement.setActions(asList("org.zstack.header.vm.APICreateVmInstanceMsg"));
        statement.setName("statement for test");
        msg.setStatements(asList(statement));
        msg.setDescription("role for test");

        return msg;
    }
}
