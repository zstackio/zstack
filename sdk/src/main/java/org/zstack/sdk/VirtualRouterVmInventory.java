package org.zstack.sdk;



public class VirtualRouterVmInventory extends org.zstack.sdk.ApplianceVmInventory {

    public java.lang.String publicNetworkUuid;
    public void setPublicNetworkUuid(java.lang.String publicNetworkUuid) {
        this.publicNetworkUuid = publicNetworkUuid;
    }
    public java.lang.String getPublicNetworkUuid() {
        return this.publicNetworkUuid;
    }

    public java.util.List<String> virtualRouterVips;
    public void setVirtualRouterVips(java.util.List<String> virtualRouterVips) {
        this.virtualRouterVips = virtualRouterVips;
    }
    public java.util.List<String> getVirtualRouterVips() {
        return this.virtualRouterVips;
    }

}
