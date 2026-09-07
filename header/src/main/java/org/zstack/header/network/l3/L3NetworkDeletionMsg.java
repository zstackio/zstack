package org.zstack.header.network.l3;

import org.zstack.header.message.DeletionMessage;
import org.zstack.header.network.l2.NetworkDeletionContext;

/**
 */
public class L3NetworkDeletionMsg extends DeletionMessage implements L3NetworkMessage {
    private String l3NetworkUuid;
    private NetworkDeletionContext networkDeletionContext;

    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }

    public NetworkDeletionContext getNetworkDeletionContext() {
        return networkDeletionContext;
    }

    public void setNetworkDeletionContext(NetworkDeletionContext networkDeletionContext) {
        this.networkDeletionContext = networkDeletionContext;
    }
}
