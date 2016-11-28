package org.zstack.test.storage.backup;

import org.zstack.header.storage.backup.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class BackupStorageChangeStateExtension implements BackupStorageChangeStateExtensionPoint {
    CLogger logger = Utils.getLogger(BackupStorageChangeStateExtensionPoint.class);
    boolean preventChange = false;
    boolean beforeCalled = false;
    boolean afterCalled = false;
    BackupStorageStateEvent expectedStateEvent;
    BackupStorageState expectedCurrent;
    BackupStorageState expectedNext;
    String expectedBackupStorageUuid;

    @Override
    public void preChangeSecondaryStorageState(BackupStorageInventory inv, BackupStorageStateEvent evt, BackupStorageState nextState) throws BackupStorageException {
        if (preventChange) {
            throw new BackupStorageException("Prevent changing backup storage state on purpose");
        }
    }

    @Override
    public void beforeChangeSecondaryStorageState(BackupStorageInventory inv, BackupStorageStateEvent evt, BackupStorageState nextState) {
        if (inv.getUuid().equals(expectedBackupStorageUuid) && inv.getState().equals(expectedCurrent.toString()) && evt == expectedStateEvent && nextState == expectedNext) {
            beforeCalled = true;
        } else {
            String err = String.format("beforeChangeSecondaryStorageState expected uuid: %s current state: %s state event: %s next state: %s but got uuid: %s current: %s event: %s next: %s", expectedBackupStorageUuid, this.expectedCurrent, this.expectedStateEvent, this.expectedNext, inv.getUuid(),
                    inv.getState(), evt, nextState);
            logger.warn(err);
        }
    }

    @Override
    public void afterChangeSecondaryStorageState(BackupStorageInventory inv, BackupStorageStateEvent evt, BackupStorageState previousState) {
        if (inv.getUuid().equals(expectedBackupStorageUuid) && inv.getState().equals(expectedNext.toString()) && evt == expectedStateEvent && previousState == expectedCurrent) {
            afterCalled = true;
        } else {
            String err = String.format("afterChangeSecondaryStorageState expected uuid: %s current state: %s state event: %s previous state : %s but got uuid: %s current: %s event: %s previous: %s", expectedBackupStorageUuid, this.expectedNext, this.expectedStateEvent, this.expectedCurrent, inv.getUuid(),
                    inv.getState(), evt, previousState);
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

    public BackupStorageStateEvent getExpectedStateEvent() {
        return expectedStateEvent;
    }

    public void setExpectedStateEvent(BackupStorageStateEvent expectedStateEvent) {
        this.expectedStateEvent = expectedStateEvent;
    }

    public BackupStorageState getExpectedCurrent() {
        return expectedCurrent;
    }

    public void setExpectedCurrent(BackupStorageState expectedCurrent) {
        this.expectedCurrent = expectedCurrent;
    }

    public BackupStorageState getExpectedNext() {
        return expectedNext;
    }

    public void setExpectedNext(BackupStorageState expectedNext) {
        this.expectedNext = expectedNext;
    }

    public String getExpectedBackupStorageUuid() {
        return expectedBackupStorageUuid;
    }

    public void setExpectedBackupStorageUuid(String expectedBackupStorageUuid) {
        this.expectedBackupStorageUuid = expectedBackupStorageUuid;
    }
}
