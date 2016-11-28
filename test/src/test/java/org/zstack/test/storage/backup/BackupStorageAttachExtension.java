package org.zstack.test.storage.backup;

import org.zstack.header.storage.backup.BackupStorageAttachExtensionPoint;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class BackupStorageAttachExtension implements BackupStorageAttachExtensionPoint {
    CLogger logger = Utils.getLogger(BackupStorageAttachExtension.class);
    boolean preventAttach = false;
    boolean beforeCalled = false;
    boolean afterCalled = false;
    String expectedBackupStorageUuid;
    String expectedZoneUuid;

    @Override
    public String preAttachBackupStorage(BackupStorageInventory inventory, String zoneUuid) {
        if (preventAttach) {
            return "Prevent attaching backup storage on purpose";
        } else {
            return null;
        }
    }

    @Override
    public void beforeAttachBackupStorage(BackupStorageInventory inventory, String zoneUuid) {
        if (inventory.getUuid().equals(expectedBackupStorageUuid) && zoneUuid.equals(expectedZoneUuid)) {
            beforeCalled = true;
        } else {
            String err = String.format("beforeAttachBackupStorage: expected backup storage uuid: %s zone uuid: %s but got backup storage uuid: %s zone uuid:%s", expectedBackupStorageUuid, expectedZoneUuid, inventory.getUuid(), zoneUuid);
            logger.warn(err);
        }
    }

    @Override
    public void failToAttachBackupStorage(BackupStorageInventory inventory, String zoneUuid) {

    }

    @Override
    public void afterAttachBackupStorage(BackupStorageInventory inventory, String zoneUuid) {
        if (inventory.getUuid().equals(expectedBackupStorageUuid) && zoneUuid.equals(expectedZoneUuid)) {
            afterCalled = true;
        } else {
            String err = String.format("afterAttachBackupStorage: expected backup storage uuid: %s zone uuid: %s but got backup storage uuid: %s zone uuid:%s", expectedBackupStorageUuid, expectedZoneUuid, inventory.getUuid(), zoneUuid);
            logger.warn(err);
        }
    }

    public boolean isPreventAttach() {
        return preventAttach;
    }

    public void setPreventAttach(boolean preventAttach) {
        this.preventAttach = preventAttach;
    }

    public boolean isBeforeCalled() {
        return beforeCalled;
    }

    public void setBeforeCalled(boolean beforeCalled) {
        this.beforeCalled = beforeCalled;
    }

    public boolean isAfterCalled() {
        return afterCalled;
    }

    public void setAfterCalled(boolean afterCalled) {
        this.afterCalled = afterCalled;
    }

    public String getExpectedBackupStorageUuid() {
        return expectedBackupStorageUuid;
    }

    public void setExpectedBackupStorageUuid(String expectedBackupStorageUuid) {
        this.expectedBackupStorageUuid = expectedBackupStorageUuid;
    }

    public String getExpectedZoneUuid() {
        return expectedZoneUuid;
    }

    public void setExpectedZoneUuid(String expectedZoneUuid) {
        this.expectedZoneUuid = expectedZoneUuid;
    }
}
