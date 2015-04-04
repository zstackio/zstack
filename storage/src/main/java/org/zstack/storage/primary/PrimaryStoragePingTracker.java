package org.zstack.storage.primary;

import org.zstack.core.tacker.PingTracker;
import org.zstack.header.message.MessageReply;
import org.zstack.header.message.NeedReplyMessage;

/**
 */
public class PrimaryStoragePingTracker extends PingTracker {
    @Override
    public String getResourceName() {
        return "primary storage";
    }

    @Override
    public NeedReplyMessage getPingMessage(String resUuid) {
        return null;
    }

    @Override
    public int getPingInterval() {
        return 0;
    }

    @Override
    public int getParallelismDegree() {
        return 0;
    }

    @Override
    public void handleReply(String resourceUuid, MessageReply reply) {

    }
}
