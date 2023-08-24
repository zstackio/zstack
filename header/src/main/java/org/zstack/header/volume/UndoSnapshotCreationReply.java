package org.zstack.header.volume;

import org.zstack.header.message.MessageReply;

public class UndoSnapshotCreationReply extends MessageReply {
    private VolumeInventory volume;

    public VolumeInventory getVolume() {
        return volume;
    }

    public void setVolume(VolumeInventory volume) {
        this.volume = volume;
    }
}
