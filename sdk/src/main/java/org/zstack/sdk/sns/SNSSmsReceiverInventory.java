package org.zstack.sdk.sns;

import org.zstack.sdk.sns.SmsReceiverType;

public class SNSSmsReceiverInventory  {

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
    }

    public java.lang.String phoneNumber;
    public void setPhoneNumber(java.lang.String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    public java.lang.String getPhoneNumber() {
        return this.phoneNumber;
    }

    public java.lang.String endpointUuid;
    public void setEndpointUuid(java.lang.String endpointUuid) {
        this.endpointUuid = endpointUuid;
    }
    public java.lang.String getEndpointUuid() {
        return this.endpointUuid;
    }

    public SmsReceiverType type;
    public void setType(SmsReceiverType type) {
        this.type = type;
    }
    public SmsReceiverType getType() {
        return this.type;
    }

}
