package org.zstack.header.image;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.rest.SDK;

import java.util.ArrayList;
import java.util.List;

/**
 */
@RestResponse(fieldsTo = {"inventory", "failures=failuresOnBackupStorage"})
public class APICreateRootVolumeTemplateFromVolumeSnapshotEvent extends APIEvent {
    @SDK(sdkClassName = "CreateRootVolumeTemplateFromVolumeSnapshotFailure")
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
