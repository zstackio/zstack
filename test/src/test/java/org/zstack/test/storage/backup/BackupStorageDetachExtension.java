package org.zstack.test.storage.backup;

import org.zstack.header.storage.backup.BackupStorageDetachExtensionPoint;
import org.zstack.header.storage.backup.BackupStorageException;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class BackupStorageDetachExtension implements BackupStorageDetachExtensionPoint {
    CLogger logger = Utils.getLogger(BackupStorageDetachExtensionPoint.class);
    boolean preventChange = false;
    boolean beforeCalled = false;
    boolean afterCalled = false;
    String expectedBackStorageUuid;
    String expectedZoneUuid;

    @Override
    public void preDetachBackupStorage(BackupStorageInventory inventory, String zoneUuid) throws BackupStorageException {
        if (preventChange) {
            throw new BackupStorageException("Prevent detaching backup storage on purpose");
        }
    }

    @Override
    public void beforeDetachBackupStorage(BackupStorageInventory inventory, String zoneUuid) {
        if (inventory.getUuid().equals(expectedBackStorageUuid) && zoneUuid.equals(expectedZoneUuid)) {
            beforeCalled = true;
        } else {
            String err = String.format("beforeDetachBackupStorage: expected backup storage uuid: %s zone uuid: %s but got backup storage uuid: %s zone uuid: %s", expectedBackStorageUuid, expectedZoneUuid, inventory.getUuid(), zoneUuid);
            logger.warn(err);
        }
    }

    @Override
    public void failToDetachBackupStorage(BackupStorageInventory inventory, String zoneUuid) {
        // TODO Auto-generated method stub

    }

    @Override
    public void afterDetachBackupStorage(BackupStorageInventory inventory, String zoneUuid) {
        if (inventory.getUuid().equals(expectedBackStorageUuid) && zoneUuid.equals(expectedZoneUuid)) {
            afterCalled = true;
        } else {
            String err = String.format("afterDetachBackupStorage: expected backup storage uuid: %s zone uuid: %s but got backup storage uuid: %s zone uuid: %s", expectedBackStorageUuid, expectedZoneUuid, inventory.getUuid(), zoneUuid);
            logger.warn(err);
        }
    }

    public boolean isPreventChange() {
        return preventChange;
    }

    public void setPreventChange(boolean preventChange) {
        this.preventChange = preventChange;
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

    public String getExpectedBackStorageUuid() {
        return expectedBackStorageUuid;
    }

    public void setExpectedBackStorageUuid(String expectedBackStorageUuid) {
        this.expectedBackStorageUuid = expectedBackStorageUuid;
    }

    public String getExpectedZoneUuid() {
        return expectedZoneUuid;
    }

    public void setExpectedZoneUuid(String expectedZoneUuid) {
        this.expectedZoneUuid = expectedZoneUuid;
    }

}
