package org.zstack.sdk;



public class HardwareL2VxlanNetworkPoolInventory extends org.zstack.sdk.L2VxlanNetworkPoolInventory {

    public java.lang.String sdnControllerUuid;
    public void setSdnControllerUuid(java.lang.String sdnControllerUuid) {
        this.sdnControllerUuid = sdnControllerUuid;
    }
    public java.lang.String getSdnControllerUuid() {
        return this.sdnControllerUuid;
    }

    public java.lang.Integer startVlan;
    public void setStartVlan(java.lang.Integer startVlan) {
        this.startVlan = startVlan;
    }
    public java.lang.Integer getStartVlan() {
        return this.startVlan;
    }

    public java.lang.Integer endVlan;
    public void setEndVlan(java.lang.Integer endVlan) {
        this.endVlan = endVlan;
    }
    public java.lang.Integer getEndVlan() {
        return this.endVlan;
    }

}
