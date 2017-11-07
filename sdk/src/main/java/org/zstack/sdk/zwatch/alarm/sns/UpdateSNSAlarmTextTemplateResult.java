package org.zstack.sdk.zwatch.alarm.sns;

import org.zstack.sdk.zwatch.alarm.sns.SNSAlarmTextTemplateInventory;

public class UpdateSNSAlarmTextTemplateResult {
    public SNSAlarmTextTemplateInventory inventory;
    public void setInventory(SNSAlarmTextTemplateInventory inventory) {
        this.inventory = inventory;
    }
    public SNSAlarmTextTemplateInventory getInventory() {
        return this.inventory;
    }

}
