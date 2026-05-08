package org.zstack.sdk.network.zns;



public class ZnsControllerInventory extends org.zstack.sdk.SdnControllerInventory {

    public java.util.List transportZones;
    public void setTransportZones(java.util.List transportZones) {
        this.transportZones = transportZones;
    }
    public java.util.List getTransportZones() {
        return this.transportZones;
    }

    public java.util.List tenants;
    public void setTenants(java.util.List tenants) {
        this.tenants = tenants;
    }
    public java.util.List getTenants() {
        return this.tenants;
    }

    public java.util.List tenantRouters;
    public void setTenantRouters(java.util.List tenantRouters) {
        this.tenantRouters = tenantRouters;
    }
    public java.util.List getTenantRouters() {
        return this.tenantRouters;
    }

}
