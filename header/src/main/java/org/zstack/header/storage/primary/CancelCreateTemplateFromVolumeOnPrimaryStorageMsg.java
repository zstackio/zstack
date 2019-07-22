package org.zstack.header.storage.primary;

import org.zstack.header.message.CancelMessage;
import org.zstack.header.volume.VolumeInventory;

import java.util.List;

/**
 * Created by MaJin on 2019/7/23.
 */
public class CancelCreateTemplateFromVolumeOnPrimaryStorageMsg extends CancelMessage implements PrimaryStorageMessage {
    private VolumeInventory volumeInventory;
    private List<String> backupStorageUuids;

    @Override
    public String getPrimaryStorageUuid() {
        return volumeInventory.getPrimaryStorageUuid();
    }

    public VolumeInventory getVolumeInventory() {
        return volumeInventory;
    }

    public void setVolumeInventory(VolumeInventory volumeInventory) {
        this.volumeInventory = volumeInventory;
    }

    public List<String> getBackupStorageUuids() {
        return backupStorageUuids;
    }

    public void setBackupStorageUuids(List<String> backupStorageUuids) {
        this.backupStorageUuids = backupStorageUuids;
    }
}
