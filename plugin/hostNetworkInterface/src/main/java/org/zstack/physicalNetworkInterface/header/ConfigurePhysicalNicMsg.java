package org.zstack.physicalNetworkInterface.header;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.physicalNetworkInterface.PhysicalNicActionType;

public class ConfigurePhysicalNicMsg extends NeedReplyMessage {
    private String physicalNicUuid;

    private PhysicalNicActionType actionType;

    private Integer virtPartNum;

    private String hostUuid;

    private boolean reconfigure;

    public String getPhysicalNicUuid() {
        return physicalNicUuid;
    }

    public void setPhysicalNicUuid(String physicalNicUuid) {
        this.physicalNicUuid = physicalNicUuid;
    }

    public PhysicalNicActionType getActionType() {
        return actionType;
    }

    public void setActionType(PhysicalNicActionType actionType) {
        this.actionType = actionType;
    }

    public Integer getVirtPartNum() {
        return virtPartNum;
    }

    public void setVirtPartNum(Integer virtPartNum) {
        this.virtPartNum = virtPartNum;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public boolean isReconfigure() {
        return reconfigure;
    }

    public void setReconfigure(boolean reconfigure) {
        this.reconfigure = reconfigure;
    }
}
