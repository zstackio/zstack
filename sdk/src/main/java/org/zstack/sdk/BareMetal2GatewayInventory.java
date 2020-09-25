package org.zstack.sdk;

import org.zstack.sdk.BareMetal2GatewayProvisionNicInventory;

public class BareMetal2GatewayInventory extends org.zstack.sdk.KVMHostInventory {

    public java.util.List attachedClusterUuids;
    public void setAttachedClusterUuids(java.util.List attachedClusterUuids) {
        this.attachedClusterUuids = attachedClusterUuids;
    }
    public java.util.List getAttachedClusterUuids() {
        return this.attachedClusterUuids;
    }

    public BareMetal2GatewayProvisionNicInventory provisionNic;
    public void setProvisionNic(BareMetal2GatewayProvisionNicInventory provisionNic) {
        this.provisionNic = provisionNic;
    }
    public BareMetal2GatewayProvisionNicInventory getProvisionNic() {
        return this.provisionNic;
    }

}
