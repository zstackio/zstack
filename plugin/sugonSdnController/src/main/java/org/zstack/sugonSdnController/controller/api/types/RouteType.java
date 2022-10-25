//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class RouteType extends ApiPropertyBase {
    String prefix;
    String next_hop;
    String next_hop_type;
    CommunityAttributes community_attributes;
    public RouteType() {
    }
    public RouteType(String prefix, String next_hop, String next_hop_type, CommunityAttributes community_attributes) {
        this.prefix = prefix;
        this.next_hop = next_hop;
        this.next_hop_type = next_hop_type;
        this.community_attributes = community_attributes;
    }
    public RouteType(String prefix) {
        this(prefix, null, null, null);    }
    public RouteType(String prefix, String next_hop) {
        this(prefix, next_hop, null, null);    }
    public RouteType(String prefix, String next_hop, String next_hop_type) {
        this(prefix, next_hop, next_hop_type, null);    }
    
    public String getPrefix() {
        return prefix;
    }
    
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }
    
    
    public String getNextHop() {
        return next_hop;
    }
    
    public void setNextHop(String next_hop) {
        this.next_hop = next_hop;
    }
    
    
    public String getNextHopType() {
        return next_hop_type;
    }
    
    public void setNextHopType(String next_hop_type) {
        this.next_hop_type = next_hop_type;
    }
    
    
    public CommunityAttributes getCommunityAttributes() {
        return community_attributes;
    }
    
    public void setCommunityAttributes(CommunityAttributes community_attributes) {
        this.community_attributes = community_attributes;
    }
    
}
