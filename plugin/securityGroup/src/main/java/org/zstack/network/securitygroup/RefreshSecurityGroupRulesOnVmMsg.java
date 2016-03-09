package org.zstack.network.securitygroup;

import org.zstack.header.message.NeedReplyMessage;

public class RefreshSecurityGroupRulesOnVmMsg extends NeedReplyMessage {
    private String vmInstanceUuid;
    private String hostUuid;
    private boolean deleteAllRules;

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public boolean isDeleteAllRules() {
        return deleteAllRules;
    }

    public void setDeleteAllRules(boolean deleteAllRules) {
        this.deleteAllRules = deleteAllRules;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
}
