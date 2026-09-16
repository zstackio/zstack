package org.zstack.header.network.l2;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.message.DeletionMessage;

/**
 * Created by weiwang on 21/03/2017.
 */
public class DeleteL2NetworkMsg extends DeletionMessage implements L2NetworkMessage {
    private String uuid;
    private NetworkDeletionContext context;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getL2NetworkUuid() {
        return uuid;
    }

    /**
     * Optional Cloud deletion context. ZNS-initiated deletes set it so the SDN extension points
     * can tell a projection cleanup from a user delete without going through the API entry.
     */
    public NetworkDeletionContext getContext() {
        return context;
    }

    public void setContext(NetworkDeletionContext context) {
        this.context = context;
    }
}
