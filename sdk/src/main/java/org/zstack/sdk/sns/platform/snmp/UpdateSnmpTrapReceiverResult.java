package org.zstack.sdk.sns.platform.snmp;

import org.zstack.sdk.sns.platform.snmp.SNSSnmpTrapReceiverInventory;

public class UpdateSnmpTrapReceiverResult {
    public SNSSnmpTrapReceiverInventory inventory;
    public void setInventory(SNSSnmpTrapReceiverInventory inventory) {
        this.inventory = inventory;
    }
    public SNSSnmpTrapReceiverInventory getInventory() {
        return this.inventory;
    }

}
