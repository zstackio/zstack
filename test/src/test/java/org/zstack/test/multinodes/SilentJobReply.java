package org.zstack.test.multinodes;

import org.zstack.header.message.MessageReply;

import java.util.List;

/**
 */
public class SilentJobReply extends MessageReply {
    private List<String> jobUuids;

    public List<String> getJobUuids() {
        return jobUuids;
    }

    public void setJobUuids(List<String> jobUuids) {
        this.jobUuids = jobUuids;
    }
}
