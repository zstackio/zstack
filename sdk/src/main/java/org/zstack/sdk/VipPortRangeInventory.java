package org.zstack.sdk;

public class VipPortRangeInventory  {

    public java.lang.String vipUuid;
    public void setVipUuid(java.lang.String vipUuid) {
        this.vipUuid = vipUuid;
    }
    public java.lang.String getVipUuid() {
        return this.vipUuid;
    }

    public java.lang.String protcol;
    public void setProtcol(java.lang.String protcol) {
        this.protcol = protcol;
    }
    public java.lang.String getProtcol() {
        return this.protcol;
    }

    public java.util.List<String> usedPorts;
    public void setUsedPorts(java.util.List<String> usedPorts) {
        this.usedPorts = usedPorts;
    }
    public java.util.List<String> getUsedPorts() {
        return this.usedPorts;
    }

}
