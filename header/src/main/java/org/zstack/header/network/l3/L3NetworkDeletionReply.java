package org.zstack.header.network.l3;

import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.NetworkDeletionContext;

/**
 */
public class L3NetworkDeletionReply extends MessageReply {
    private boolean coordinatedSourceDeletion;
    private NetworkDeletionContext networkDeletionContext;

    public NetworkDeletionContext getNetworkDeletionContext() { return networkDeletionContext; }
    public void setNetworkDeletionContext(NetworkDeletionContext value) { networkDeletionContext = value; }

    public boolean isCoordinatedSourceDeletion() { return coordinatedSourceDeletion; }
    public void setCoordinatedSourceDeletion(boolean value) { coordinatedSourceDeletion = value; }
}
