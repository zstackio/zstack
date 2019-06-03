package org.zstack.header.host;

/**
 */
public class ConnectHostInfo {
    private boolean isNewAdded;
    private boolean isStartPingTaskOnFailure;
    private boolean isSkipInstallVirtPkgs;

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

    public boolean isSkipInstallVirtPkgs() {
        return isSkipInstallVirtPkgs;
    }

    public void setSkipInstallVirtPkgs(boolean skipInstallVirtPkgs) {
        isSkipInstallVirtPkgs = skipInstallVirtPkgs;
    }

    public static ConnectHostInfo fromConnectHostMsg(ConnectHostMsg msg) {
        ConnectHostInfo info = new ConnectHostInfo();
        info.setNewAdded(msg.isNewAdd());
        info.setStartPingTaskOnFailure(msg.isStartPingTaskOnFailure());
        return info;
    }
}
