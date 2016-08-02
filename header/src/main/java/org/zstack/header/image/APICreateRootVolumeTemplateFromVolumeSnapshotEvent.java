package org.zstack.header.image;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.APIEvent;

import java.util.ArrayList;
import java.util.List;

/**
 */
public class APICreateRootVolumeTemplateFromVolumeSnapshotEvent extends APIEvent {
    public static class Failure {
        public String backupStorageUuid;
        public ErrorCode error;
    }

    private ImageInventory inventory;
    private List<Failure> failuresOnBackupStorage;

    public void addFailure(Failure failure) {
        if (failuresOnBackupStorage == null) {
            failuresOnBackupStorage = new ArrayList<>();
        }
        failuresOnBackupStorage.add(failure);
    }

    public List<Failure> getFailuresOnBackupStorage() {
        return failuresOnBackupStorage;
    }

    public void setFailuresOnBackupStorage(List<Failure> failuresOnBackupStorage) {
        this.failuresOnBackupStorage = failuresOnBackupStorage;
    }

    public APICreateRootVolumeTemplateFromVolumeSnapshotEvent(String apiId) {
        super(apiId);
    }

    public APICreateRootVolumeTemplateFromVolumeSnapshotEvent() {
        super(null);
    }

    public ImageInventory getInventory() {
        return inventory;
    }

    public void setInventory(ImageInventory inventory) {
        this.inventory = inventory;
    }
}
