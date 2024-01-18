package org.zstack.sdk;

import org.zstack.sdk.OTPDeviceInfoInventory;

public class UpdateOTPDevicePrivateDataResult {
    public OTPDeviceInfoInventory inventory;
    public void setInventory(OTPDeviceInfoInventory inventory) {
        this.inventory = inventory;
    }
    public OTPDeviceInfoInventory getInventory() {
        return this.inventory;
    }

}
