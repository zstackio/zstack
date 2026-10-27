package org.zstack.kvm.memory;

import org.zstack.header.message.NeedReplyMessage;

/** Releases a VM's unresolved migration hold after the operator confirmed where the VM runs; fails unless released. */
public class MemoryVmMigrationHoldRecoveryMsg extends NeedReplyMessage {
    private String vmUuid;
    private String confirmedHostUuid;

    public String getVmUuid() { return vmUuid; }
    public void setVmUuid(String value) { vmUuid = value; }
    public String getConfirmedHostUuid() { return confirmedHostUuid; }
    public void setConfirmedHostUuid(String value) { confirmedHostUuid = value; }
}
