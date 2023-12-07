package org.zstack.sdk;

import org.zstack.sdk.L2PortGroupVlanMode;

public class L2PortGroupNetworkInventory extends org.zstack.sdk.L2NetworkInventory {

    public java.lang.String vSwitchUuid;
    public void setVSwitchUuid(java.lang.String vSwitchUuid) {
        this.vSwitchUuid = vSwitchUuid;
    }
    public java.lang.String getVSwitchUuid() {
        return this.vSwitchUuid;
    }

    public L2PortGroupVlanMode vlanMode;
    public void setVlanMode(L2PortGroupVlanMode vlanMode) {
        this.vlanMode = vlanMode;
    }
    public L2PortGroupVlanMode getVlanMode() {
        return this.vlanMode;
    }

    public java.lang.Integer vlanId;
    public void setVlanId(java.lang.Integer vlanId) {
        this.vlanId = vlanId;
    }
    public java.lang.Integer getVlanId() {
        return this.vlanId;
    }

    public java.lang.String vlanRanges;
    public void setVlanRanges(java.lang.String vlanRanges) {
        this.vlanRanges = vlanRanges;
    }
    public java.lang.String getVlanRanges() {
        return this.vlanRanges;
    }

}
