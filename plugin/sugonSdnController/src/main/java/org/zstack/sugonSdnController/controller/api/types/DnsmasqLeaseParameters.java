//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class DnsmasqLeaseParameters extends ApiPropertyBase {
    Integer lease_expiry_time;
    String client_id;
    public DnsmasqLeaseParameters() {
    }
    public DnsmasqLeaseParameters(Integer lease_expiry_time, String client_id) {
        this.lease_expiry_time = lease_expiry_time;
        this.client_id = client_id;
    }
    public DnsmasqLeaseParameters(Integer lease_expiry_time) {
        this(lease_expiry_time, null);    }
    
    public Integer getLeaseExpiryTime() {
        return lease_expiry_time;
    }
    
    public void setLeaseExpiryTime(Integer lease_expiry_time) {
        this.lease_expiry_time = lease_expiry_time;
    }
    
    
    public String getClientId() {
        return client_id;
    }
    
    public void setClientId(String client_id) {
        this.client_id = client_id;
    }
    
}
