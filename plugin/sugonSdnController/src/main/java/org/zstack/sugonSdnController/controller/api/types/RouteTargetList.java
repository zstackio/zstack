//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import java.util.List;
import java.util.ArrayList;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class RouteTargetList extends ApiPropertyBase {
    List<String> route_target;
    public RouteTargetList() {
    }
    public RouteTargetList(List<String> route_target) {
        this.route_target = route_target;
    }
    
    public List<String> getRouteTarget() {
        return route_target;
    }
    
    
    public void addRouteTarget(String obj) {
        if (route_target == null) {
            route_target = new ArrayList<String>();
        }
        route_target.add(obj);
    }
    public void clearRouteTarget() {
        route_target = null;
    }
    
}
