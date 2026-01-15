package org.zstack.sdk;

import org.zstack.sdk.ConsistencyCheckStatus;

public class CheckPrimaryStorageConsistencyResult {
    public boolean consistent;
    public void setConsistent(boolean consistent) {
        this.consistent = consistent;
    }
    public boolean getConsistent() {
        return this.consistent;
    }

    public ConsistencyCheckStatus status;
    public void setStatus(ConsistencyCheckStatus status) {
        this.status = status;
    }
    public ConsistencyCheckStatus getStatus() {
        return this.status;
    }

    public java.lang.String candidateVgUuid;
    public void setCandidateVgUuid(java.lang.String candidateVgUuid) {
        this.candidateVgUuid = candidateVgUuid;
    }
    public java.lang.String getCandidateVgUuid() {
        return this.candidateVgUuid;
    }

}
