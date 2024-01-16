package org.zstack.header.host;

import org.zstack.header.message.NeedReplyMessage;

public class ConnectHostMsg extends NeedReplyMessage implements HostMessage {
    private String uuid;
    private boolean isStartPingTaskOnFailure;
    private boolean newAdd;
    private boolean calledByAPI;

    public ConnectHostMsg() {
    }

    public ConnectHostMsg(String uuid) {
        super();
        this.uuid = uuid;
    }

    public boolean isNewAdd() {
        return newAdd;
    }

    public void setNewAdd(boolean newAdd) {
        this.newAdd = newAdd;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public boolean isStartPingTaskOnFailure() {
        return isStartPingTaskOnFailure;
    }

    public void setStartPingTaskOnFailure(boolean isStartPingTaskOnFailure) {
        this.isStartPingTaskOnFailure = isStartPingTaskOnFailure;
    }

    public boolean isCalledByAPI() {
        return calledByAPI;
    }

    public void setCalledByAPI(boolean calledByAPI) {
        this.calledByAPI = calledByAPI;
    }

    @Override
    public String getHostUuid() {
        return getUuid();
    }
}
