package org.zstack.sdk.sns.platform.aliyunsms;

import org.zstack.sdk.sns.SNSSmsEndpointInventory;

public class CreateSNSAliyunSmsEndpointResult {
    public SNSSmsEndpointInventory inventory;
    public void setInventory(SNSSmsEndpointInventory inventory) {
        this.inventory = inventory;
    }
    public SNSSmsEndpointInventory getInventory() {
        return this.inventory;
    }

}
