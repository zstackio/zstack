package org.zstack.network.securitygroup;

import org.zstack.header.message.Message;

public class RefreshSecurityGroupRulesOnHostMsg extends Message {
    private String hostUuid;

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }
}
