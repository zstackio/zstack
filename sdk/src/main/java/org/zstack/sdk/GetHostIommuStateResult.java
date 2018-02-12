package org.zstack.sdk;

public class GetHostIommuStateResult {
    public HostIommuStateType state;
    public void setState(HostIommuStateType state) {
        this.state = state;
    }
    public HostIommuStateType getState() {
        return this.state;
    }

}
