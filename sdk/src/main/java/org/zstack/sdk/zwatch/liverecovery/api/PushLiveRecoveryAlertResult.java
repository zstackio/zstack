package org.zstack.sdk.zwatch.liverecovery.api;



public class PushLiveRecoveryAlertResult {
    public java.lang.String remoteEventId;
    public void setRemoteEventId(java.lang.String remoteEventId) {
        this.remoteEventId = remoteEventId;
    }
    public java.lang.String getRemoteEventId() {
        return this.remoteEventId;
    }

    public java.lang.String remoteAlertId;
    public void setRemoteAlertId(java.lang.String remoteAlertId) {
        this.remoteAlertId = remoteAlertId;
    }
    public java.lang.String getRemoteAlertId() {
        return this.remoteAlertId;
    }

    public boolean duplicate;
    public void setDuplicate(boolean duplicate) {
        this.duplicate = duplicate;
    }
    public boolean getDuplicate() {
        return this.duplicate;
    }

    public boolean stale;
    public void setStale(boolean stale) {
        this.stale = stale;
    }
    public boolean getStale() {
        return this.stale;
    }

}
