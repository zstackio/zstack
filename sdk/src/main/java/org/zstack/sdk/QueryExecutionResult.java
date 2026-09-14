package org.zstack.sdk;



public class QueryExecutionResult {
    public java.util.List inventories;
    public void setInventories(java.util.List inventories) {
        this.inventories = inventories;
    }
    public java.util.List getInventories() {
        return this.inventories;
    }

    public java.lang.Long total;
    public void setTotal(java.lang.Long total) {
        this.total = total;
    }
    public java.lang.Long getTotal() {
        return this.total;
    }

    public java.lang.String nextCursor;
    public void setNextCursor(java.lang.String nextCursor) {
        this.nextCursor = nextCursor;
    }
    public java.lang.String getNextCursor() {
        return this.nextCursor;
    }

}
