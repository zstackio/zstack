package org.zstack.sdk;

public class ChangeIPSecConnectionStateResult {
    public IPsecConnectionInventory inventory;
    public void setInventory(IPsecConnectionInventory inventory) {
        this.inventory = inventory;
    }
    public IPsecConnectionInventory getInventory() {
        return this.inventory;
    }

}
