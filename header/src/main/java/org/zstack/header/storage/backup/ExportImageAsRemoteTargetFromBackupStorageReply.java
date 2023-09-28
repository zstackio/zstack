package org.zstack.header.storage.backup;

import org.zstack.header.message.MessageReply;

public class ExportImageAsRemoteTargetFromBackupStorageReply extends MessageReply {
    private RemoteTarget remoteTarget;

    public RemoteTarget getRemoteTarget() {
        return remoteTarget;
    }

    public void setRemoteTarget(RemoteTarget remoteTarget) {
        this.remoteTarget = remoteTarget;
    }
}

