package org.zstack.header.network;

import org.zstack.header.message.MessageReply;

public class PrepareZnsSegmentMutationReply extends MessageReply {
    private long acceptedConfigVersion;

    public long getAcceptedConfigVersion() {
        return acceptedConfigVersion;
    }

    public void setAcceptedConfigVersion(long acceptedConfigVersion) {
        this.acceptedConfigVersion = acceptedConfigVersion;
    }
}
