//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class SubnetType extends ApiPropertyBase {
    String ip_prefix;
    Integer ip_prefix_len;
    public SubnetType() {
    }
    public SubnetType(String ip_prefix, Integer ip_prefix_len) {
        this.ip_prefix = ip_prefix;
        this.ip_prefix_len = ip_prefix_len;
    }
    public SubnetType(String ip_prefix) {
        this(ip_prefix, null);    }
    
    public String getIpPrefix() {
        return ip_prefix;
    }
    
    public void setIpPrefix(String ip_prefix) {
        this.ip_prefix = ip_prefix;
    }
    
    
    public Integer getIpPrefixLen() {
        return ip_prefix_len;
    }
    
    public void setIpPrefixLen(Integer ip_prefix_len) {
        this.ip_prefix_len = ip_prefix_len;
    }
    
}
