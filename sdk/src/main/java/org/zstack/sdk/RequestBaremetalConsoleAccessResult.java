package org.zstack.sdk;

import org.zstack.sdk.BaremetalConsoleProxyInventory;

public class RequestBaremetalConsoleAccessResult {
    public BaremetalConsoleProxyInventory inventory;
    public void setInventory(BaremetalConsoleProxyInventory inventory) {
        this.inventory = inventory;
    }
    public BaremetalConsoleProxyInventory getInventory() {
        return this.inventory;
    }

}
