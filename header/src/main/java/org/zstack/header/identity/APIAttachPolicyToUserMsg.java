package org.zstack.header.identity;

import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;

@NeedRoles(roles = {IdentityRoles.ATTACH_POLICY_TO_USER_ROLE})
public class APIAttachPolicyToUserMsg extends APIMessage implements AccountMessage {
    @APIParam
    private String userUuid;
    @APIParam
    private String policyUuid;
    
    @Override
    public String getAccountUuid() {
        return this.getSession().getAccountUuid();
    }

    public String getUserUuid() {
        return userUuid;
    }

    public void setUserUuid(String userUuid) {
        this.userUuid = userUuid;
    }

    public String getPolicyUuid() {
        return policyUuid;
    }

    public void setPolicyUuid(String policyUuid) {
        this.policyUuid = policyUuid;
    }
}
