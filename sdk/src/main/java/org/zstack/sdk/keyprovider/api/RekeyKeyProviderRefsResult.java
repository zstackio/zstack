package org.zstack.sdk.keyprovider.api;



public class RekeyKeyProviderRefsResult {
    public int totalCount;
    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }
    public int getTotalCount() {
        return this.totalCount;
    }

    public int successCount;
    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }
    public int getSuccessCount() {
        return this.successCount;
    }

    public int skippedCount;
    public void setSkippedCount(int skippedCount) {
        this.skippedCount = skippedCount;
    }
    public int getSkippedCount() {
        return this.skippedCount;
    }

    public java.util.List skippedResources;
    public void setSkippedResources(java.util.List skippedResources) {
        this.skippedResources = skippedResources;
    }
    public java.util.List getSkippedResources() {
        return this.skippedResources;
    }

}
