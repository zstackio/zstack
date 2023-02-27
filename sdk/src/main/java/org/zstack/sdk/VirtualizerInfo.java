package org.zstack.sdk;



public class VirtualizerInfo  {

    public java.lang.String category;
    public void setCategory(java.lang.String category) {
        this.category = category;
    }
    public java.lang.String getCategory() {
        return this.category;
    }

    public java.lang.String currentQemuVersion;
    public void setCurrentQemuVersion(java.lang.String currentQemuVersion) {
        this.currentQemuVersion = currentQemuVersion;
    }
    public java.lang.String getCurrentQemuVersion() {
        return this.currentQemuVersion;
    }

    public java.lang.String expectQemuVersion;
    public void setExpectQemuVersion(java.lang.String expectQemuVersion) {
        this.expectQemuVersion = expectQemuVersion;
    }
    public java.lang.String getExpectQemuVersion() {
        return this.expectQemuVersion;
    }

    public boolean matched;
    public void setMatched(boolean matched) {
        this.matched = matched;
    }
    public boolean getMatched() {
        return this.matched;
    }

}
