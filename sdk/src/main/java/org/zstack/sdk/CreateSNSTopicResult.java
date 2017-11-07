package org.zstack.sdk;

import org.zstack.sdk.SNSTopicInventory;

public class CreateSNSTopicResult {
    public SNSTopicInventory inventory;
    public void setInventory(SNSTopicInventory inventory) {
        this.inventory = inventory;
    }
    public SNSTopicInventory getInventory() {
        return this.inventory;
    }

}
