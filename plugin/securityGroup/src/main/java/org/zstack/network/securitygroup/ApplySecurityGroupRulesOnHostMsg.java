package org.zstack.network.securitygroup;

import org.zstack.header.host.HostMessage;
import org.zstack.header.message.NeedReplyMessage;

public class ApplySecurityGroupRulesOnHostMsg extends NeedReplyMessage implements HostMessage {
    private String hostUuid;
    private String securityGroupUuid;

    @Override
    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getSecurityGroupUuid() {
        return securityGroupUuid;
    }

    public void setSecurityGroupUuid(String securityGroupUuid) {
        this.securityGroupUuid = securityGroupUuid;
    }
}
