package org.zstack.header.network.l3;

import org.zstack.header.message.MessageReply;

/**
 */
public class IpRangeDeletionReply extends MessageReply {
    private boolean coordinatedNetworkConfigFailure;

    public boolean isCoordinatedNetworkConfigFailure() {
        return coordinatedNetworkConfigFailure;
    }

    public void setCoordinatedNetworkConfigFailure(boolean value) {
        coordinatedNetworkConfigFailure = value;
    }
}
