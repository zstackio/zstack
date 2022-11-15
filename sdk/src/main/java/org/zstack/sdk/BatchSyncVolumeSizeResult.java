package org.zstack.sdk;



public class BatchSyncVolumeSizeResult {
    public Integer successCount;
    public void setSuccessCount(Integer successCount) {
        this.successCount = successCount;
    }
    public Integer getSuccessCount() {
        return this.successCount;
    }

    public Integer failCount;
    public void setFailCount(Integer failCount) {
        this.failCount = failCount;
    }
    public Integer getFailCount() {
        return this.failCount;
    }

}
