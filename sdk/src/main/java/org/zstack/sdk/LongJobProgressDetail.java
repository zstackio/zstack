package org.zstack.sdk;



public class LongJobProgressDetail  {

    public java.lang.Integer percent;
    public void setPercent(java.lang.Integer percent) {
        this.percent = percent;
    }
    public java.lang.Integer getPercent() {
        return this.percent;
    }

    public java.lang.String stage;
    public void setStage(java.lang.String stage) {
        this.stage = stage;
    }
    public java.lang.String getStage() {
        return this.stage;
    }

    public java.lang.String state;
    public void setState(java.lang.String state) {
        this.state = state;
    }
    public java.lang.String getState() {
        return this.state;
    }

    public java.lang.String stateReason;
    public void setStateReason(java.lang.String stateReason) {
        this.stateReason = stateReason;
    }
    public java.lang.String getStateReason() {
        return this.stateReason;
    }

    public java.lang.Long processedBytes;
    public void setProcessedBytes(java.lang.Long processedBytes) {
        this.processedBytes = processedBytes;
    }
    public java.lang.Long getProcessedBytes() {
        return this.processedBytes;
    }

    public java.lang.Long totalBytes;
    public void setTotalBytes(java.lang.Long totalBytes) {
        this.totalBytes = totalBytes;
    }
    public java.lang.Long getTotalBytes() {
        return this.totalBytes;
    }

    public java.lang.Long processedItems;
    public void setProcessedItems(java.lang.Long processedItems) {
        this.processedItems = processedItems;
    }
    public java.lang.Long getProcessedItems() {
        return this.processedItems;
    }

    public java.lang.Long totalItems;
    public void setTotalItems(java.lang.Long totalItems) {
        this.totalItems = totalItems;
    }
    public java.lang.Long getTotalItems() {
        return this.totalItems;
    }

    public java.lang.Long speedBytesPerSecond;
    public void setSpeedBytesPerSecond(java.lang.Long speedBytesPerSecond) {
        this.speedBytesPerSecond = speedBytesPerSecond;
    }
    public java.lang.Long getSpeedBytesPerSecond() {
        return this.speedBytesPerSecond;
    }

    public java.lang.Long estimatedRemainingSeconds;
    public void setEstimatedRemainingSeconds(java.lang.Long estimatedRemainingSeconds) {
        this.estimatedRemainingSeconds = estimatedRemainingSeconds;
    }
    public java.lang.Long getEstimatedRemainingSeconds() {
        return this.estimatedRemainingSeconds;
    }

    public java.util.LinkedHashMap extra;
    public void setExtra(java.util.LinkedHashMap extra) {
        this.extra = extra;
    }
    public java.util.LinkedHashMap getExtra() {
        return this.extra;
    }

}
