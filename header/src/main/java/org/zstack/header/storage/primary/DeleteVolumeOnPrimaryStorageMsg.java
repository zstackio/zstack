package org.zstack.header.storage.primary;

import org.zstack.header.core.ApiTimeout;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.vm.APIExpungeVmInstanceMsg;
import org.zstack.header.volume.VolumeInventory;

@ApiTimeout(apiClasses = {APIExpungeVmInstanceMsg.class})
public class DeleteVolumeOnPrimaryStorageMsg extends NeedReplyMessage implements PrimaryStorageMessage {
    private String uuid;
    private VolumeInventory volume;

    @Override
    public String getPrimaryStorageUuid() {
        return getUuid();
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public VolumeInventory getVolume() {
        return volume;
    }

    public void setVolume(VolumeInventory volume) {
        this.volume = volume;
    }
}
