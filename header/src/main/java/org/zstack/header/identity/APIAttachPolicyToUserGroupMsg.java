package org.zstack.header.identity;

import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
@NeedRoles(roles = {IdentityRoles.ATTACH_POLICY_TO_USER_GROUP_ROLE})
public class APIAttachPolicyToUserGroupMsg extends APIMessage implements AccountMessage {
    @APIParam
    private String policyUuid;
    @APIParam
    private String groupUuid;
    
    @Override
    public String getAccountUuid() {
        return this.getSession().getAccountUuid();
    }

    public String getPolicyUuid() {
        return policyUuid;
    }

    public void setPolicyUuid(String policyUuid) {
        this.policyUuid = policyUuid;
    }

    public String getGroupUuid() {
        return groupUuid;
    }

    public void setGroupUuid(String groupUuid) {
        this.groupUuid = groupUuid;
    }
}
