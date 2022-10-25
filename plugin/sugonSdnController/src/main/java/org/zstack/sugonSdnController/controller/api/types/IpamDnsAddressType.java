//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class IpamDnsAddressType extends ApiPropertyBase {
    IpAddressesType tenant_dns_server_address;
    String virtual_dns_server_name;
    public IpamDnsAddressType() {
    }
    public IpamDnsAddressType(IpAddressesType tenant_dns_server_address, String virtual_dns_server_name) {
        this.tenant_dns_server_address = tenant_dns_server_address;
        this.virtual_dns_server_name = virtual_dns_server_name;
    }
    public IpamDnsAddressType(IpAddressesType tenant_dns_server_address) {
        this(tenant_dns_server_address, null);    }
    
    public IpAddressesType getTenantDnsServerAddress() {
        return tenant_dns_server_address;
    }
    
    public void setTenantDnsServerAddress(IpAddressesType tenant_dns_server_address) {
        this.tenant_dns_server_address = tenant_dns_server_address;
    }
    
    
    public String getVirtualDnsServerName() {
        return virtual_dns_server_name;
    }
    
    public void setVirtualDnsServerName(String virtual_dns_server_name) {
        this.virtual_dns_server_name = virtual_dns_server_name;
    }
    
}
