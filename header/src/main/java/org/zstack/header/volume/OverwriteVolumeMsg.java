package org.zstack.header.volume;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by MaJin on 2019/4/2.
 */
public class OverwriteVolumeMsg extends NeedReplyMessage implements VolumeMessage {
    private VolumeInventory volume;
    private VolumeInventory transientVolume;

    @Override
    public String getVolumeUuid() {
        return volume.getUuid();
    }
    public VolumeInventory getTransientVolume() {
        return transientVolume;
    }

    public void setTransientVolume(VolumeInventory transientVolume) {
        this.transientVolume = transientVolume;
    }

    public void setVolume(VolumeInventory volume) {
        this.volume = volume;
    }

    public VolumeInventory getVolume() {
        return volume;
    }
}
