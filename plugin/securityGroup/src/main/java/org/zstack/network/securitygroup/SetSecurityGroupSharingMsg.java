package org.zstack.network.securitygroup;

import org.zstack.header.identity.SessionInventory;
import org.zstack.header.message.NeedReplyMessage;

import java.util.List;

public class SetSecurityGroupSharingMsg extends NeedReplyMessage implements SecurityGroupMessage {
    private String securityGroupUuid;
    private String mode;
    private List<String> accountUuids;
    private SessionInventory session;

    @Override
    public String getSecurityGroupUuid() {
        return securityGroupUuid;
    }

    public void setSecurityGroupUuid(String securityGroupUuid) {
        this.securityGroupUuid = securityGroupUuid;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public List<String> getAccountUuids() {
        return accountUuids;
    }

    public void setAccountUuids(List<String> accountUuids) {
        this.accountUuids = accountUuids;
    }

    public SessionInventory getSession() {
        return session;
    }

    public void setSession(SessionInventory session) {
        this.session = session;
    }
}
