//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class DiscoveryPubSubEndPointType extends ApiPropertyBase {
    String ep_type;
    String ep_id;
    SubnetType ep_prefix;
    String ep_version;
    public DiscoveryPubSubEndPointType() {
    }
    public DiscoveryPubSubEndPointType(String ep_type, String ep_id, SubnetType ep_prefix, String ep_version) {
        this.ep_type = ep_type;
        this.ep_id = ep_id;
        this.ep_prefix = ep_prefix;
        this.ep_version = ep_version;
    }
    public DiscoveryPubSubEndPointType(String ep_type) {
        this(ep_type, null, null, null);    }
    public DiscoveryPubSubEndPointType(String ep_type, String ep_id) {
        this(ep_type, ep_id, null, null);    }
    public DiscoveryPubSubEndPointType(String ep_type, String ep_id, SubnetType ep_prefix) {
        this(ep_type, ep_id, ep_prefix, null);    }
    
    public String getEpType() {
        return ep_type;
    }
    
    public void setEpType(String ep_type) {
        this.ep_type = ep_type;
    }
    
    
    public String getEpId() {
        return ep_id;
    }
    
    public void setEpId(String ep_id) {
        this.ep_id = ep_id;
    }
    
    
    public SubnetType getEpPrefix() {
        return ep_prefix;
    }
    
    public void setEpPrefix(SubnetType ep_prefix) {
        this.ep_prefix = ep_prefix;
    }
    
    
    public String getEpVersion() {
        return ep_version;
    }
    
    public void setEpVersion(String ep_version) {
        this.ep_version = ep_version;
    }
    
}
