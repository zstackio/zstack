package org.zstack.kvm.memory;

import org.zstack.header.message.NeedReplyMessage;

/** One Host's pause, resume, drain or writeback preparation; the reply fails unless it succeeded. */
public class MemoryHostOperationMsg extends NeedReplyMessage {
    private String hostUuid;
    private String action;
    private String candidateId;
    private String jobUuid;
    private String accountUuid;
    private String userUuid;

    public String getHostUuid() { return hostUuid; }
    public void setHostUuid(String value) { hostUuid = value; }
    public String getAction() { return action; }
    public void setAction(String value) { action = value; }
    public String getCandidateId() { return candidateId; }
    public void setCandidateId(String value) { candidateId = value; }
    public String getJobUuid() { return jobUuid; }
    public void setJobUuid(String value) { jobUuid = value; }
    public String getAccountUuid() { return accountUuid; }
    public void setAccountUuid(String value) { accountUuid = value; }
    public String getUserUuid() { return userUuid; }
    public void setUserUuid(String value) { userUuid = value; }
}
