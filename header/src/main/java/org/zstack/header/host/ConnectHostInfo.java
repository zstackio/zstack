package org.zstack.header.host;

/**
 */
public class ConnectHostInfo {
    private boolean isNewAdded;
    private boolean isStartPingTaskOnFailure;
    private boolean reconnect;

    // do not install these packages when connect host
    private String skipPackages;

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

    public boolean isReconnect() {
        return reconnect;
    }

    public void setReconnect(boolean reconnect) {
        this.reconnect = reconnect;
    }

    public String getSkipPackages() {
        return skipPackages;
    }

    public void setSkipPackages(String skipPackages) {
        this.skipPackages = skipPackages;
    }

    public static ConnectHostInfo fromConnectHostMsg(ConnectHostMsg msg) {
        ConnectHostInfo info = new ConnectHostInfo();
        info.setNewAdded(msg.isNewAdd());
        info.setStartPingTaskOnFailure(msg.isStartPingTaskOnFailure());
        info.setReconnect(msg.isReconnect());
        return info;
    }
}
