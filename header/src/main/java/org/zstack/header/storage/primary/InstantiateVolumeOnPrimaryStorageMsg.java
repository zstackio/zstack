package org.zstack.header.storage.primary;

import org.zstack.header.host.HostInventory;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.volume.VolumeInventory;

public class InstantiateVolumeOnPrimaryStorageMsg extends NeedReplyMessage implements PrimaryStorageMessage {
    private HostInventory destHost;
    private VolumeInventory volume;
    private String primaryStorageUuid;

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public VolumeInventory getVolume() {
        return volume;
    }

    public void setVolume(VolumeInventory volume) {
        this.volume = volume;
    }

    @Override
    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public HostInventory getDestHost() {
        return destHost;
    }

    public void setDestHost(HostInventory destHost) {
        this.destHost = destHost;
    }
}
