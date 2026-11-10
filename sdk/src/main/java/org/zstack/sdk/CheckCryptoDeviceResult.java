package org.zstack.sdk;

import org.zstack.sdk.CryptoDeviceInventory;

public class CheckCryptoDeviceResult {
    public CryptoDeviceInventory inventory;
    public void setInventory(CryptoDeviceInventory inventory) {
        this.inventory = inventory;
    }
    public CryptoDeviceInventory getInventory() {
        return this.inventory;
    }

}
