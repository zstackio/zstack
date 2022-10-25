//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import java.util.List;
import java.util.ArrayList;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class LogicalRouterPRListType extends ApiPropertyBase {
    List<LogicalRouterPRListParams> logical_router_list;
    public LogicalRouterPRListType() {
    }
    public LogicalRouterPRListType(List<LogicalRouterPRListParams> logical_router_list) {
        this.logical_router_list = logical_router_list;
    }
    
    public List<LogicalRouterPRListParams> getLogicalRouterList() {
        return logical_router_list;
    }
    
    
    public void addLogicalRouter(LogicalRouterPRListParams obj) {
        if (logical_router_list == null) {
            logical_router_list = new ArrayList<LogicalRouterPRListParams>();
        }
        logical_router_list.add(obj);
    }
    public void clearLogicalRouter() {
        logical_router_list = null;
    }
    
    
    public void addLogicalRouter(String logical_router_uuid, List<String> physical_router_uuid_list) {
        if (logical_router_list == null) {
            logical_router_list = new ArrayList<LogicalRouterPRListParams>();
        }
        logical_router_list.add(new LogicalRouterPRListParams(logical_router_uuid, physical_router_uuid_list));
    }
    
}
