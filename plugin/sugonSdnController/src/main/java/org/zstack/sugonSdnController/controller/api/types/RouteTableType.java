//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import java.util.List;
import java.util.ArrayList;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class RouteTableType extends ApiPropertyBase {
    List<RouteType> route;
    public RouteTableType() {
    }
    public RouteTableType(List<RouteType> route) {
        this.route = route;
    }
    
    public List<RouteType> getRoute() {
        return route;
    }
    
    
    public void addRoute(RouteType obj) {
        if (route == null) {
            route = new ArrayList<RouteType>();
        }
        route.add(obj);
    }
    public void clearRoute() {
        route = null;
    }
    
    
    public void addRoute(String prefix, String next_hop, String next_hop_type, CommunityAttributes community_attributes) {
        if (route == null) {
            route = new ArrayList<RouteType>();
        }
        route.add(new RouteType(prefix, next_hop, next_hop_type, community_attributes));
    }
    
}
