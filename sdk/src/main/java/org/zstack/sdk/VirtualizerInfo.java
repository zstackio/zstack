package org.zstack.sdk;

public class VirtualizerInfo  {

    public java.lang.String hypervisor;
    public void setHypervisor(java.lang.String hypervisor) {
        this.hypervisor = hypervisor;
    }
    public java.lang.String getHypervisor() {
        return this.hypervisor;
    }

    public java.lang.String currentVersion;
    public void setCurrentVersion(java.lang.String currentVersion) {
        this.currentVersion = currentVersion;
    }
    public java.lang.String getCurrentVersion() {
        return this.currentVersion;
    }

    public java.lang.String expectVersion;
    public void setExpectVersion(java.lang.String expectVersion) {
        this.expectVersion = expectVersion;
    }
    public java.lang.String getExpectVersion() {
        return this.expectVersion;
    }

    public VirtualizerVersionState matchState;
    public void setMatchState(VirtualizerVersionState matchState) {
        this.matchState = matchState;
    }
    public VirtualizerVersionState getMatchState() {
        return this.matchState;
    }

}
