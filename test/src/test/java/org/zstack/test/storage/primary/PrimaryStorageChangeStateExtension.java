package org.zstack.test.storage.primary;

import org.zstack.header.storage.primary.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class PrimaryStorageChangeStateExtension implements PrimaryStorageChangeStateExtensionPoint {
    CLogger logger = Utils.getLogger(PrimaryStorageChangeStateExtension.class);
    boolean beforeCalled = false;
    boolean afterCalled = false;
    boolean preventChange = false;
    PrimaryStorageState expectedCurrent;
    PrimaryStorageState expectedNext;
    PrimaryStorageStateEvent expectedStateEvent;

    @Override
    public void preChangePrimaryStorageState(PrimaryStorageInventory inv, PrimaryStorageStateEvent evt, PrimaryStorageState nextState) throws PrimaryStorageException {
        if (this.isPreventChange()) {
            throw new PrimaryStorageException("Prevent changing primary storage state on purpose");
        }
    }

    @Override
    public void beforeChangePrimaryStorageState(PrimaryStorageInventory inv, PrimaryStorageStateEvent evt, PrimaryStorageState nextState) {
        if (inv.getState().equals(this.expectedCurrent.toString()) && evt == this.expectedStateEvent && nextState == this.expectedNext) {
            this.beforeCalled = true;
        } else {
            String err = String.format("beforeChangePrimaryStorageState: expected current state: %s state event: %s next state: %s but got current: %s event: %s next: %s", this.expectedCurrent, this.expectedStateEvent, this.expectedNext, inv.getState(), evt, nextState);
            logger.warn(err);
        }
    }

    @Override
    public void afterChangePrimaryStorageState(PrimaryStorageInventory inv, PrimaryStorageStateEvent evt, PrimaryStorageState previousState) {
        if (inv.getState().equals(this.expectedNext.toString()) && evt == this.expectedStateEvent && previousState == this.expectedCurrent) {
            this.afterCalled = true;
        } else {
            String err = String.format("afterChangePrimaryStorageState: expected current state: %s state event: %s previous state: %s but got current: %s event: %s previous: %s", this.expectedNext, this.expectedStateEvent, this.expectedCurrent, inv.getState(), evt, previousState);
            logger.warn(err);
        }
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

    public boolean isPreventChange() {
        return preventChange;
    }

    public void setPreventChange(boolean preventChange) {
        this.preventChange = preventChange;
    }

    public PrimaryStorageState getExpectedCurrent() {
        return expectedCurrent;
    }

    public void setExpectedCurrent(PrimaryStorageState expectedCurrent) {
        this.expectedCurrent = expectedCurrent;
    }

    public PrimaryStorageState getExpectedNext() {
        return expectedNext;
    }

    public void setExpectedNext(PrimaryStorageState expectedNext) {
        this.expectedNext = expectedNext;
    }

    public PrimaryStorageStateEvent getExpectedStateEvent() {
        return expectedStateEvent;
    }

    public void setExpectedStateEvent(PrimaryStorageStateEvent expectedStateEvent) {
        this.expectedStateEvent = expectedStateEvent;
    }
}
