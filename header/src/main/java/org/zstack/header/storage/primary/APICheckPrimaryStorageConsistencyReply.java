package org.zstack.header.storage.primary;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

@RestResponse(fieldsTo = {"all"})
public class APICheckPrimaryStorageConsistencyReply extends APIReply {
    private boolean consistent;
    private ConsistencyCheckStatus status;
    private String candidateVgUuid;

    public boolean isConsistent() {
        return consistent;
    }

    public void setConsistent(boolean consistent) {
        this.consistent = consistent;
    }

    public ConsistencyCheckStatus getStatus() {
        return status;
    }

    public void setStatus(ConsistencyCheckStatus status) {
        this.status = status;
    }

    public String getCandidateVgUuid() {
        return candidateVgUuid;
    }

    public void setCandidateVgUuid(String candidateVgUuid) {
        this.candidateVgUuid = candidateVgUuid;
    }

    public static APICheckPrimaryStorageConsistencyReply __example__() {
        APICheckPrimaryStorageConsistencyReply reply = new APICheckPrimaryStorageConsistencyReply();
        reply.setConsistent(true);
        reply.setStatus(ConsistencyCheckStatus.CONSISTENT);
        return reply;
    }
}
