package org.zstack.sdk;

import org.zstack.sdk.MiniNetworkConfigStruct;
import org.zstack.sdk.MiniNetworkConfigStruct;

public class MiniHostInfo  {

    public java.lang.String sn;
    public void setSn(java.lang.String sn) {
        this.sn = sn;
    }
    public java.lang.String getSn() {
        return this.sn;
    }

    public MiniNetworkConfigStruct ipmi;
    public void setIpmi(MiniNetworkConfigStruct ipmi) {
        this.ipmi = ipmi;
    }
    public MiniNetworkConfigStruct getIpmi() {
        return this.ipmi;
    }

    public MiniNetworkConfigStruct mgmt;
    public void setMgmt(MiniNetworkConfigStruct mgmt) {
        this.mgmt = mgmt;
    }
    public MiniNetworkConfigStruct getMgmt() {
        return this.mgmt;
    }

}
