package org.zstack.sdk;

import org.zstack.sdk.NfvInstInventory;

public class CreateNfvInstResult {
    public NfvInstInventory inventory;
    public void setInventory(NfvInstInventory inventory) {
        this.inventory = inventory;
    }
    public NfvInstInventory getInventory() {
        return this.inventory;
    }

}
