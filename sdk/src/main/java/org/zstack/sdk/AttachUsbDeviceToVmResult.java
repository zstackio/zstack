package org.zstack.sdk;

public class AttachUsbDeviceToVmResult {
    public UsbDeviceInventory inventory;
    public void setInventory(UsbDeviceInventory inventory) {
        this.inventory = inventory;
    }
    public UsbDeviceInventory getInventory() {
        return this.inventory;
    }

}
