package org.zstack.header.storage.primary;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.volume.VolumeInventory;

import java.util.ArrayList;
import java.util.List;

public class RollbackVolumeEncryptionOnPrimaryStorageMsg extends NeedReplyMessage implements PrimaryStorageMessage {
    private String hostUuid;
    private VolumeInventory volume;
    private List<ConvertVolumeEncryptionOnPrimaryStorageMsg.VolumeEncryptionConversionItem> items = new ArrayList<>();

    @Override
    public String getPrimaryStorageUuid() {
        return volume.getPrimaryStorageUuid();
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public VolumeInventory getVolume() {
        return volume;
    }

    public void setVolume(VolumeInventory volume) {
        this.volume = volume;
    }

    public List<ConvertVolumeEncryptionOnPrimaryStorageMsg.VolumeEncryptionConversionItem> getItems() {
        return items;
    }

    public void setItems(List<ConvertVolumeEncryptionOnPrimaryStorageMsg.VolumeEncryptionConversionItem> items) {
        this.items = items;
    }
}
