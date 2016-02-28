package org.zstack.header.identity;

import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
@Action(category = AccountConstant.ACTION_CATEGORY, accountOnly = true)
public class APIAttachPolicyToUserGroupMsg extends APIMessage implements AccountMessage {
    @APIParam(resourceType = PolicyVO.class, checkAccount = true, operationTarget = true)
    private String policyUuid;
    @APIParam(resourceType = UserGroupVO.class, checkAccount = true, operationTarget = true)
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
