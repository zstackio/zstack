package org.zstack.sdk;

import org.zstack.sdk.ZsDatasetTransferInventory;

public class OpenZsDatasetTransferResult {
    public ZsDatasetTransferInventory inventory;
    public void setInventory(ZsDatasetTransferInventory inventory) {
        this.inventory = inventory;
    }
    public ZsDatasetTransferInventory getInventory() {
        return this.inventory;
    }

    public java.lang.String ticket;
    public void setTicket(java.lang.String ticket) {
        this.ticket = ticket;
    }
    public java.lang.String getTicket() {
        return this.ticket;
    }

    public java.lang.String endpoint;
    public void setEndpoint(java.lang.String endpoint) {
        this.endpoint = endpoint;
    }
    public java.lang.String getEndpoint() {
        return this.endpoint;
    }

    public long maxBytes;
    public void setMaxBytes(long maxBytes) {
        this.maxBytes = maxBytes;
    }
    public long getMaxBytes() {
        return this.maxBytes;
    }

}
