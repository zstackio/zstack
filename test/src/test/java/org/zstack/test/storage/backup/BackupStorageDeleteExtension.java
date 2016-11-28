package org.zstack.test.storage.backup;

import org.zstack.header.storage.backup.BackupStorageDeleteExtensionPoint;
import org.zstack.header.storage.backup.BackupStorageException;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class BackupStorageDeleteExtension implements BackupStorageDeleteExtensionPoint {
    CLogger logger = Utils.getLogger(BackupStorageDeleteExtensionPoint.class);
    boolean preventDelete = false;
    boolean beforeCalled = false;
    boolean afterCalled = false;
    String expectedBackupStorageUuid;

    @Override
    public void preDeleteSecondaryStorage(BackupStorageInventory inv) throws BackupStorageException {
        if (preventDelete) {
            throw new BackupStorageException("Prevent deleting backup storage on purpose");
        }
    }

    @Override
    public void beforeDeleteSecondaryStorage(BackupStorageInventory inv) {
        if (inv.getUuid().equals(expectedBackupStorageUuid)) {
            beforeCalled = true;
        } else {
            String err = String.format("beforeDeleteSecondaryStorage: expect back storage uuid:%s but got %s", expectedBackupStorageUuid, inv.getUuid());
            logger.warn(err);
        }
    }

    @Override
    public void afterDeleteSecondaryStorage(BackupStorageInventory inv) {
        if (inv.getUuid().equals(expectedBackupStorageUuid)) {
            afterCalled = true;
        } else {
            String err = String.format("afterDeleteSecondaryStorage: expect back storage uuid:%s but got %s", expectedBackupStorageUuid, inv.getUuid());
            logger.warn(err);
        }
    }

    public CLogger getLogger() {
        return logger;
    }

    public void setLogger(CLogger logger) {
        this.logger = logger;
    }

    public boolean isPreventDelete() {
        return preventDelete;
    }

    public void setPreventDelete(boolean preventDelete) {
        this.preventDelete = preventDelete;
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
}
