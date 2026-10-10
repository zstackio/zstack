package org.zstack.sdk;



public class QueryMemoryStateResult {
    public java.util.List inventories;
    public void setInventories(java.util.List inventories) {
        this.inventories = inventories;
    }
    public java.util.List getInventories() {
        return this.inventories;
    }

    public long total;
    public void setTotal(long total) {
        this.total = total;
    }
    public long getTotal() {
        return this.total;
    }

    public java.lang.Integer nextPage;
    public void setNextPage(java.lang.Integer nextPage) {
        this.nextPage = nextPage;
    }
    public java.lang.Integer getNextPage() {
        return this.nextPage;
    }

    public java.lang.String snapshotId;
    public void setSnapshotId(java.lang.String snapshotId) {
        this.snapshotId = snapshotId;
    }
    public java.lang.String getSnapshotId() {
        return this.snapshotId;
    }

}
