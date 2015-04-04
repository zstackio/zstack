package org.zstack.network.securitygroup;

import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;

/**
 */
public class APIGetCandidateVmNicForSecurityGroupMsg extends APISyncCallMessage {
    @APIParam(resourceType = SecurityGroupVO.class)
    private String securityGroupUuid;

    public String getSecurityGroupUuid() {
        return securityGroupUuid;
    }

    public void setSecurityGroupUuid(String securityGroupUuid) {
        this.securityGroupUuid = securityGroupUuid;
    }
}
