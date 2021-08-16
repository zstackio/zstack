package org.zstack.header.volume;

import org.zstack.header.message.MessageReply;

/**
 * Created by xing5 on 2016/8/22.
 */
public class InstantiateVolumeReply extends MessageReply {
    private VolumeInventory volume;
    private String allocatedInstallUrl;

    public String getAllocatedInstallUrl() {
        return allocatedInstallUrl;
    }

    public void setAllocatedInstallUrl(String allocatedInstallUrl) {
        this.allocatedInstallUrl = allocatedInstallUrl;
    }

    public VolumeInventory getVolume() {
        return volume;
    }

    public void setVolume(VolumeInventory volume) {
        this.volume = volume;
    }
}
