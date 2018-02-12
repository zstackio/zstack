package org.zstack.sdk;

import org.zstack.sdk.VpcVpnIkeConfigStruct;
import org.zstack.sdk.VpcVpnIpSecConfigStruct;

public class GetVpcVpnConfigurationFromRemoteResult {
    public VpcVpnIkeConfigStruct ikeConf;
    public void setIkeConf(VpcVpnIkeConfigStruct ikeConf) {
        this.ikeConf = ikeConf;
    }
    public VpcVpnIkeConfigStruct getIkeConf() {
        return this.ikeConf;
    }

    public VpcVpnIpSecConfigStruct ipSecConf;
    public void setIpSecConf(VpcVpnIpSecConfigStruct ipSecConf) {
        this.ipSecConf = ipSecConf;
    }
    public VpcVpnIpSecConfigStruct getIpSecConf() {
        return this.ipSecConf;
    }

}
