package org.zstack.sdk;

import org.zstack.sdk.Concurrency;
import org.zstack.sdk.RecordBudget;

public class MemoryHostOperationsInventory  {

    public java.lang.String hostUuid;
    public void setHostUuid(java.lang.String hostUuid) {
        this.hostUuid = hostUuid;
    }
    public java.lang.String getHostUuid() {
        return this.hostUuid;
    }

    public java.util.List entries;
    public void setEntries(java.util.List entries) {
        this.entries = entries;
    }
    public java.util.List getEntries() {
        return this.entries;
    }

    public java.lang.Long total;
    public void setTotal(java.lang.Long total) {
        this.total = total;
    }
    public java.lang.Long getTotal() {
        return this.total;
    }

    public java.lang.Integer nextPage;
    public void setNextPage(java.lang.Integer nextPage) {
        this.nextPage = nextPage;
    }
    public java.lang.Integer getNextPage() {
        return this.nextPage;
    }

    public Concurrency concurrency;
    public void setConcurrency(Concurrency concurrency) {
        this.concurrency = concurrency;
    }
    public Concurrency getConcurrency() {
        return this.concurrency;
    }

    public RecordBudget recordBudget;
    public void setRecordBudget(RecordBudget recordBudget) {
        this.recordBudget = recordBudget;
    }
    public RecordBudget getRecordBudget() {
        return this.recordBudget;
    }

}
