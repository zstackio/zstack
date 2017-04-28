package org.zstack.sdk;

public class SecurityGroupIngressRuleTO extends SecurityGroupRuleInventory {

    public java.util.List<String> friendCidrs;
    public void setFriendCidrs(java.util.List<String> friendCidrs) {
        this.friendCidrs = friendCidrs;
    }
    public java.util.List<String> getFriendCidrs() {
        return this.friendCidrs;
    }

}
