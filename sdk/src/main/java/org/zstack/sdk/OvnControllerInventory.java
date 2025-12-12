package org.zstack.sdk;



public class OvnControllerInventory extends org.zstack.sdk.SdnControllerInventory {

    public boolean remoteOvn;
    public void setRemoteOvn(boolean remoteOvn) {
        this.remoteOvn = remoteOvn;
    }
    public boolean getRemoteOvn() {
        return this.remoteOvn;
    }

}
