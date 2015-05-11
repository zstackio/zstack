package org.zstack.header.identity;

import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;

@NeedRoles(roles = {IdentityRoles.CREATE_POLICY_ROLE})
public class APICreatePolicyMsg extends APICreateMessage implements AccountMessage {
    @APIParam
    private String name;
    private String description;
    @APIParam
    private String policyData;
    
    @Override
    public String getAccountUuid() {
        return this.getSession().getAccountUuid();
    }

    public String getPolicyData() {
        return policyData;
    }

    public void setPolicyData(String policyData) {
        this.policyData = policyData;
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
}
