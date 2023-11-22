package org.zstack.sdk;

import org.zstack.sdk.HostNetworkInterfaceLldpRefInventory;

public class QueryHostNetworkInterfaceLldpRefResult {
    public HostNetworkInterfaceLldpRefInventory inventory;
    public void setInventory(HostNetworkInterfaceLldpRefInventory inventory) {
        this.inventory = inventory;
    }
    public HostNetworkInterfaceLldpRefInventory getInventory() {
        return this.inventory;
    }

    public java.lang.Long total;
    public void setTotal(java.lang.Long total) {
        this.total = total;
    }
    public java.lang.Long getTotal() {
        return this.total;
    }

}
