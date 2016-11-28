package org.zstack.test.storage.primary;

import org.zstack.header.storage.primary.PrimaryStorageDeleteExtensionPoint;
import org.zstack.header.storage.primary.PrimaryStorageException;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class PrimaryStorageDeleteExtension implements PrimaryStorageDeleteExtensionPoint {
    CLogger logger = Utils.getLogger(PrimaryStorageDeleteExtension.class);
    boolean preventDelete = false;
    boolean beforeCalled = false;
    boolean afterCalled = false;
    String expectedUuid;

    @Override
    public void preDeletePrimaryStorage(PrimaryStorageInventory inv) throws PrimaryStorageException {
        if (this.preventDelete) {
            throw new PrimaryStorageException("Prevent deleting primary storage on purpose");
        }
    }

    @Override
    public void beforeDeletePrimaryStorage(PrimaryStorageInventory inv) {
        if (this.expectedUuid.equals(inv.getUuid())) {
            this.beforeCalled = true;
        } else {
            String err = String.format("beforeDeletePrimaryStorage: expected uuid: %s but got %s", this.expectedUuid, inv.getUuid());
            logger.warn(err);
        }
    }

    @Override
    public void afterDeletePrimaryStorage(PrimaryStorageInventory inv) {
        if (this.expectedUuid.equals(inv.getUuid())) {
            this.afterCalled = true;
        } else {
            String err = String.format("afterDeletePrimaryStorage: expected uuid: %s but got %s", this.expectedUuid, inv.getUuid());
            logger.warn(err);
        }
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

    public String getExpectedUuid() {
        return expectedUuid;
    }

    public void setExpectedUuid(String expectedUuid) {
        this.expectedUuid = expectedUuid;
    }
}
