package org.zstack.kvm.memory;

import org.zstack.header.message.NeedReplyMessage;

/** Pause, resume or drain the KVM Hosts of a Cluster (or one of them), one Host at a time. */
public class MemoryClusterOperationMsg extends NeedReplyMessage {
    private String clusterUuid;
    private String hostUuid;
    private String action;
    private String accountUuid;
    private String userUuid;

    public String getClusterUuid() { return clusterUuid; }
    public void setClusterUuid(String value) { clusterUuid = value; }
    public String getHostUuid() { return hostUuid; }
    public void setHostUuid(String value) { hostUuid = value; }
    public String getAction() { return action; }
    public void setAction(String value) { action = value; }
    public String getAccountUuid() { return accountUuid; }
    public void setAccountUuid(String value) { accountUuid = value; }
    public String getUserUuid() { return userUuid; }
    public void setUserUuid(String value) { userUuid = value; }
}
