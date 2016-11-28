package org.zstack.test.compute.cluster;

import org.zstack.header.cluster.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class ClusteChangeStateExtension implements ClusterChangeStateExtensionPoint {
    CLogger logger = Utils.getLogger(ClusteChangeStateExtension.class);
    private ClusterState expectedCurrent;
    private ClusterState expectedNext;
    private ClusterStateEvent expectedStateEvent;
    private boolean isBeforeSuccess = false;
    private boolean isAfterSuccess = false;
    private boolean preventChange = false;

    @Override
    public void preChangeClusterState(ClusterInventory inventory, ClusterStateEvent event, ClusterState nextState) throws ClusterException {
        if (this.preventChange) {
            throw new ClusterException("Prevent changing cluster state on purpose");
        }
    }

    @Override
    public void beforeChangeClusterState(ClusterInventory inventory, ClusterStateEvent event, ClusterState nextState) {
        if (inventory.getState().equals(this.expectedCurrent.toString()) && event == this.expectedStateEvent && nextState == this.expectedNext) {
            this.isBeforeSuccess = true;
        } else {
            String err = String.format("beforeChangeClusterState: expected current state: %s state event:%s next state:%s, but got current:%s event: %s next :%s", this.expectedCurrent, this.expectedStateEvent, this.expectedNext, inventory.getState(), event, nextState);
            logger.warn(err);
        }
    }

    @Override
    public void afterChangeClusterState(ClusterInventory inventory, ClusterStateEvent event, ClusterState previousState) {
        if (inventory.getState().equals(this.expectedNext.toString()) && event == this.expectedStateEvent && previousState == this.expectedCurrent) {
            this.isAfterSuccess = true;
        } else {
            String err = String.format("afterChangeClusterState: expected previous state: %s state event:%s current state:%s, but got previous:%s event: %s current :%s", this.expectedCurrent, this.expectedStateEvent, this.expectedNext, previousState, event, inventory.getState());
            logger.warn(err);
        }
    }

    public ClusterState getExpectedCurrent() {
        return expectedCurrent;
    }

    public void setExpectedCurrent(ClusterState expectedCurrent) {
        this.expectedCurrent = expectedCurrent;
    }

    public ClusterState getExpectedNext() {
        return expectedNext;
    }

    public void setExpectedNext(ClusterState expectedNext) {
        this.expectedNext = expectedNext;
    }

    public ClusterStateEvent getExpectedStateEvent() {
        return expectedStateEvent;
    }

    public void setExpectedStateEvent(ClusterStateEvent expectedStateEvent) {
        this.expectedStateEvent = expectedStateEvent;
    }

    public boolean isBeforeSuccess() {
        return isBeforeSuccess;
    }

    public void setBeforeSuccess(boolean isBeforeSuccess) {
        this.isBeforeSuccess = isBeforeSuccess;
    }

    public boolean isAfterSuccess() {
        return isAfterSuccess;
    }

    public void setAfterSuccess(boolean isAfterSuccess) {
        this.isAfterSuccess = isAfterSuccess;
    }

    public boolean isPreventChange() {
        return preventChange;
    }

    public void setPreventChange(boolean preventChange) {
        this.preventChange = preventChange;
    }
}
