package org.zstack.network.securitygroup;

import org.zstack.header.identity.Action;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;

/**
 */
@Action(category = SecurityGroupConstant.ACTION_CATEGORY, names = {"read"})
public class APIGetCandidateVmNicForSecurityGroupMsg extends APISyncCallMessage {
    @APIParam(resourceType = SecurityGroupVO.class, checkAccount = true)
    private String securityGroupUuid;

    public String getSecurityGroupUuid() {
        return securityGroupUuid;
    }

    public void setSecurityGroupUuid(String securityGroupUuid) {
        this.securityGroupUuid = securityGroupUuid;
    }
}
