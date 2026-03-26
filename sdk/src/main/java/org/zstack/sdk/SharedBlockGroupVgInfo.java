package org.zstack.sdk;



public class SharedBlockGroupVgInfo  {

    public java.util.List candidateLuns;
    public void setCandidateLuns(java.util.List candidateLuns) {
        this.candidateLuns = candidateLuns;
    }
    public java.util.List getCandidateLuns() {
        return this.candidateLuns;
    }

    public boolean sharedGroupComplete;
    public void setSharedGroupComplete(boolean sharedGroupComplete) {
        this.sharedGroupComplete = sharedGroupComplete;
    }
    public boolean getSharedGroupComplete() {
        return this.sharedGroupComplete;
    }

    public java.util.Map existLunWwidsByHost;
    public void setExistLunWwidsByHost(java.util.Map existLunWwidsByHost) {
        this.existLunWwidsByHost = existLunWwidsByHost;
    }
    public java.util.Map getExistLunWwidsByHost() {
        return this.existLunWwidsByHost;
    }

}
