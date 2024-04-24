package org.zstack.sdk.sns.platform.universalsms;

import org.zstack.sdk.sns.platform.universalsms.SNSUniversalSmsEndpointInventory;

public class CreateSNSUniversalSmsEndpointResult {
    public SNSUniversalSmsEndpointInventory inventory;
    public void setInventory(SNSUniversalSmsEndpointInventory inventory) {
        this.inventory = inventory;
    }
    public SNSUniversalSmsEndpointInventory getInventory() {
        return this.inventory;
    }

}
