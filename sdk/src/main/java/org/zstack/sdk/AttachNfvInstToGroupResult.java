package org.zstack.sdk;

import org.zstack.sdk.NfvInstGroupInventory;

public class AttachNfvInstToGroupResult {
    public NfvInstGroupInventory inventory;
    public void setInventory(NfvInstGroupInventory inventory) {
        this.inventory = inventory;
    }
    public NfvInstGroupInventory getInventory() {
        return this.inventory;
    }

}
