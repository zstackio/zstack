package org.zstack.header.host;

/**
 */
public class ConnectHostInfo {
    private boolean isNewAdded;
    private boolean isStartPingTaskOnFailure;

    public boolean isNewAdded() {
        return isNewAdded;
    }

    public void setNewAdded(boolean isNewAdded) {
        this.isNewAdded = isNewAdded;
    }

    public boolean isStartPingTaskOnFailure() {
        return isStartPingTaskOnFailure;
    }

    public void setStartPingTaskOnFailure(boolean isStartPingTaskOnFailure) {
        this.isStartPingTaskOnFailure = isStartPingTaskOnFailure;
    }

    public static ConnectHostInfo fromConnectHostMsg(ConnectHostMsg msg) {
        ConnectHostInfo info = new ConnectHostInfo();
        info.setNewAdded(msg.isNewAdd());
        info.setStartPingTaskOnFailure(msg.isStartPingTaskOnFailure());
        return info;
    }
}
