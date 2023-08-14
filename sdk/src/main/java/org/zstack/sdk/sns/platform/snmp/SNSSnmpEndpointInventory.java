package org.zstack.sdk.sns.platform.snmp;

import org.zstack.sdk.sns.platform.snmp.SNSSnmpTrapReceiverInventory;

public class SNSSnmpEndpointInventory extends org.zstack.sdk.sns.SNSApplicationEndpointInventory {

    public SNSSnmpTrapReceiverInventory trapReceiver;
    public void setTrapReceiver(SNSSnmpTrapReceiverInventory trapReceiver) {
        this.trapReceiver = trapReceiver;
    }
    public SNSSnmpTrapReceiverInventory getTrapReceiver() {
        return this.trapReceiver;
    }

}
