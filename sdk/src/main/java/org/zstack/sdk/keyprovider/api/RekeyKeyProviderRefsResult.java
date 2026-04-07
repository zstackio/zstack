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

    public int failedCount;
    public void setFailedCount(int failedCount) {
        this.failedCount = failedCount;
    }
    public int getFailedCount() {
        return this.failedCount;
    }

    public java.util.List skippedResources;
    public void setSkippedResources(java.util.List skippedResources) {
        this.skippedResources = skippedResources;
    }
    public java.util.List getSkippedResources() {
        return this.skippedResources;
    }

    public java.util.List failedResources;
    public void setFailedResources(java.util.List failedResources) {
        this.failedResources = failedResources;
    }
    public java.util.List getFailedResources() {
        return this.failedResources;
    }

}
