package org.zstack.sdk;

import org.zstack.sdk.MemorySummaryInventory;

public class GetMemorySummaryResult {
    public MemorySummaryInventory summary;
    public void setSummary(MemorySummaryInventory summary) {
        this.summary = summary;
    }
    public MemorySummaryInventory getSummary() {
        return this.summary;
    }

}
