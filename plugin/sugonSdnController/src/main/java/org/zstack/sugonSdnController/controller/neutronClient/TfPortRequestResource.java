package org.zstack.sugonSdnController.controller.neutronClient;

import java.util.List;

public class TfPortRequestResource {
    private String tenant_id;

    private String network_id;

    private String subnet_id;

    private List<TfPortIpEntity> fixed_ips;

    private String mac_address;

    public String getMacAddress() {
        return mac_address;
    }

    public void setMacAddress(String mac_address) {
        this.mac_address = mac_address;
    }

    public String getTenantId() {
        return tenant_id;
    }

    public void setTenantId(String tenantId) {
        this.tenant_id = tenantId;
    }

    public String getNetworkId() {
        return network_id;
    }

    public void setNetworkId(String networkId) {
        this.network_id = networkId;
    }

    public String getSubnetId() {
        return subnet_id;
    }

    public void setSubnetId(String subnetId) {
        this.subnet_id = subnetId;
    }

    public List<TfPortIpEntity> getFixdIps() {
        return fixed_ips;
    }

    public void setFixdIps(List<TfPortIpEntity> fixdIps) {
        this.fixed_ips = fixdIps;
    }
}
