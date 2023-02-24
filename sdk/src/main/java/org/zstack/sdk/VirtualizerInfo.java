package org.zstack.sdk;

import org.zstack.sdk.VirtualizerVersionState;

public class VirtualizerInfo  {

    public java.lang.String category;
    public void setCategory(java.lang.String category) {
        this.category = category;
    }
    public java.lang.String getCategory() {
        return this.category;
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
