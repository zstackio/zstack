package org.zstack.header.vm;

import org.zstack.header.message.Message;

public class ChangeVmStateMsg extends Message implements VmInstanceMessage {
    private String vmInstanceUuid;
    private String stateEvent;
    
    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public String getStateEvent() {
        return stateEvent;
    }

    public void setStateEvent(String stateEvent) {
        this.stateEvent = stateEvent;
    }


    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
}
