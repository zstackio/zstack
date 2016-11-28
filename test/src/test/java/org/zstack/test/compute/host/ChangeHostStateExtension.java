package org.zstack.test.compute.host;

import org.zstack.header.host.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class ChangeHostStateExtension implements HostChangeStateExtensionPoint {
    private CLogger logger = Utils.getLogger(ChangeHostStateExtension.class);
    private HostState expectedCurrent;
    private HostState expectedNext;
    private HostStateEvent expectedStateEvent;
    private boolean beforeCalled = false;
    private boolean afterCalled = false;
    private boolean preventChange = false;

    @Override
    public void preChangeHostState(HostInventory inventory, HostStateEvent event, HostState nextState) throws HostException {
        if (this.preventChange) {
            throw new HostException("Prevent changing host state on purpose");
        }
    }

    @Override
    public void beforeChangeHostState(HostInventory inventory, HostStateEvent event, HostState nextState) {
        if (inventory.getState().equals(this.expectedCurrent.toString()) && event == this.expectedStateEvent && nextState == this.expectedNext) {
            this.beforeCalled = true;
        } else {
            String err = String.format("beforeChangeHostState: expected current state: %s state event: %s next state: %s but got current: %s event: %s next: %s", this.expectedCurrent, this.expectedStateEvent, this.expectedNext, inventory.getState(), event, nextState);
            logger.warn(err);
        }
    }

    @Override
    public void afterChangeHostState(HostInventory inventory, HostStateEvent event, HostState previousState) {
        if (inventory.getState().equals(this.expectedNext.toString()) && event == this.expectedStateEvent && previousState == this.expectedCurrent) {
            this.afterCalled = true;
        } else {
            String err = String.format("afterChangeHostState: expected current state: %s state event: %s previous state: %s but got current: %s event: %s previous: %s", this.expectedNext, this.expectedStateEvent, this.expectedCurrent, inventory.getState(), event, previousState);
            logger.warn(err);
        }
    }

    public HostState getExpectedCurrent() {
        return expectedCurrent;
    }

    public void setExpectedCurrent(HostState expectedCurrent) {
        this.expectedCurrent = expectedCurrent;
    }

    public HostState getExpectedNext() {
        return expectedNext;
    }

    public void setExpectedNext(HostState expectedNext) {
        this.expectedNext = expectedNext;
    }

    public HostStateEvent getExpectedStateEvent() {
        return expectedStateEvent;
    }

    public void setExpectedStateEvent(HostStateEvent expectedStateEvent) {
        this.expectedStateEvent = expectedStateEvent;
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
}
