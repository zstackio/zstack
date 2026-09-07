package org.zstack.sdk;



public class CleanAiHostModelCacheResult {
    public java.util.List cleanedInventories;
    public void setCleanedInventories(java.util.List cleanedInventories) {
        this.cleanedInventories = cleanedInventories;
    }
    public java.util.List getCleanedInventories() {
        return this.cleanedInventories;
    }

    public java.util.List skippedReasons;
    public void setSkippedReasons(java.util.List skippedReasons) {
        this.skippedReasons = skippedReasons;
    }
    public java.util.List getSkippedReasons() {
        return this.skippedReasons;
    }

    public java.lang.Long bytesReclaimed;
    public void setBytesReclaimed(java.lang.Long bytesReclaimed) {
        this.bytesReclaimed = bytesReclaimed;
    }
    public java.lang.Long getBytesReclaimed() {
        return this.bytesReclaimed;
    }

}
