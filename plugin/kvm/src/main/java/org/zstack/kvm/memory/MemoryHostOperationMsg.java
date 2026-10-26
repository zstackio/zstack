package org.zstack.kvm.memory;

import org.zstack.header.message.NeedReplyMessage;

/** One Host's writeback preparation or uncertain-result recovery; the reply fails unless it succeeded. */
public class MemoryHostOperationMsg extends NeedReplyMessage {
    private String hostUuid;
    private String action;
    private MemoryBackendPreparation preparation;
    private MemoryUncertainRecovery recovery;
    private String expectedControlOperationUuid;
    private String jobUuid;
    private String accountUuid;
    private String userUuid;

    public String getHostUuid() { return hostUuid; }
    public void setHostUuid(String value) { hostUuid = value; }
    public String getAction() { return action; }
    public void setAction(String value) { action = value; }
    public MemoryBackendPreparation getPreparation() { return preparation; }
    public void setPreparation(MemoryBackendPreparation value) { preparation = value; }
    public MemoryUncertainRecovery getRecovery() { return recovery; }
    public void setRecovery(MemoryUncertainRecovery value) { recovery = value; }
    public String getExpectedControlOperationUuid() { return expectedControlOperationUuid; }
    public void setExpectedControlOperationUuid(String value) { expectedControlOperationUuid = value; }
    public String getJobUuid() { return jobUuid; }
    public void setJobUuid(String value) { jobUuid = value; }
    public String getAccountUuid() { return accountUuid; }
    public void setAccountUuid(String value) { accountUuid = value; }
    public String getUserUuid() { return userUuid; }
    public void setUserUuid(String value) { userUuid = value; }
}
