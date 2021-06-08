package org.zstack.sdk.zwatch.api;



public class AlertStatistics  {

    public long maxCreateTime;
    public void setMaxCreateTime(long maxCreateTime) {
        this.maxCreateTime = maxCreateTime;
    }
    public long getMaxCreateTime() {
        return this.maxCreateTime;
    }

    public long maxId;
    public void setMaxId(long maxId) {
        this.maxId = maxId;
    }
    public long getMaxId() {
        return this.maxId;
    }

    public long count;
    public void setCount(long count) {
        this.count = count;
    }
    public long getCount() {
        return this.count;
    }

    public java.util.List tags;
    public void setTags(java.util.List tags) {
        this.tags = tags;
    }
    public java.util.List getTags() {
        return this.tags;
    }

}
