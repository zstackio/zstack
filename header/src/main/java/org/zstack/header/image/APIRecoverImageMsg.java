package org.zstack.header.image;

import org.zstack.header.message.APIMessage;

import java.util.List;

/**
 * Created by frank on 11/15/2015.
 */
public class APIRecoverImageMsg extends APIMessage implements ImageMessage {
    private String imageUuid;
    private List<String> backupStorageUuids;

    @Override
    public String getImageUuid() {
        return imageUuid;
    }

    public void setImageUuid(String imageUuid) {
        this.imageUuid = imageUuid;
    }

    public List<String> getBackupStorageUuids() {
        return backupStorageUuids;
    }

    public void setBackupStorageUuids(List<String> backupStorageUuids) {
        this.backupStorageUuids = backupStorageUuids;
    }
}
