package org.zstack.portal.managementnode;

import org.zstack.header.message.MessageReply;

public class ApplyManagementNodeResourceControlReply extends MessageReply {
    private boolean synced;

    public boolean isSynced() {
        return synced;
    }

    public void setSynced(boolean synced) {
        this.synced = synced;
    }
}
