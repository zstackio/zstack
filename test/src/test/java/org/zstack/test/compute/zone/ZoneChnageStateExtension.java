package org.zstack.test.compute.zone;

import org.zstack.header.zone.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class ZoneChnageStateExtension implements ZoneChangeStateExtensionPoint {
    CLogger logger = Utils.getLogger(ZoneChnageStateExtension.class);
    private boolean beforeCalled = false;
    private boolean afterCalled = false;
    private boolean isPreventChange = true;

    private ZoneState expectedCurrent;
    private ZoneState expectedNext;
    private ZoneStateEvent expectedStateEvent;

    @Override
    public void preChangeZoneState(ZoneInventory inventory, ZoneStateEvent event, ZoneState nextState) throws ZoneException {
        if (this.isPreventChange) {
            throw new ZoneException("Prevent changing on purpose");
        }
    }

    @Override
    public void beforeChangeZoneState(ZoneInventory inventory, ZoneStateEvent event, ZoneState nextState) {
        if (inventory.getState().equals(expectedCurrent.toString()) && event == this.expectedStateEvent && nextState == this.expectedNext) {
            this.beforeCalled = true;
        } else {
            logger.warn(String.format(
                    "beforeChangeZoneState: expected current state: %s stateEvent: %s nextState:%s, but received current:%s stateEventL:%s next:%s",
                    this.expectedCurrent, this.expectedStateEvent, this.expectedNext, inventory.getState(), event, nextState));
        }
    }

    @Override
    public void afterChangeZoneState(ZoneInventory inventory, ZoneStateEvent event, ZoneState previousState) {
        if (inventory.getState().equals(expectedNext.toString()) && event == this.expectedStateEvent && this.expectedCurrent == previousState) {
            this.afterCalled = true;
        } else {
            logger.warn(String.format(
                    "afterChangeZoneState: expected current state: %s stateEvent: %s previousState:%s, but received current:%s stateEventL:%s previous:%s",
                    this.expectedNext, this.expectedStateEvent, this.expectedCurrent, inventory.getState(), event, previousState));
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

    public ZoneState getExpectedCurrent() {
        return expectedCurrent;
    }

    public void setExpectedCurrent(ZoneState expectedCurrent) {
        this.expectedCurrent = expectedCurrent;
    }

    public ZoneState getExpectedNext() {
        return expectedNext;
    }

    public void setExpectedNext(ZoneState expectedNext) {
        this.expectedNext = expectedNext;
    }

    public boolean isPreventChange() {
        return isPreventChange;
    }

    public void setPreventChange(boolean isPreventChange) {
        this.isPreventChange = isPreventChange;
    }

    public ZoneStateEvent getExpectedStateEvent() {
        return expectedStateEvent;
    }

    public void setExpectedStateEvent(ZoneStateEvent expectedStateEvent) {
        this.expectedStateEvent = expectedStateEvent;
    }
}
