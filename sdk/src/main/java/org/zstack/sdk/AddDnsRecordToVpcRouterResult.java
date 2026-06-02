package org.zstack.sdk;

import org.zstack.sdk.VpcRouterDnsRecordInventory;

public class AddDnsRecordToVpcRouterResult {
    public VpcRouterDnsRecordInventory inventory;
    public void setInventory(VpcRouterDnsRecordInventory inventory) {
        this.inventory = inventory;
    }
    public VpcRouterDnsRecordInventory getInventory() {
        return this.inventory;
    }

}
