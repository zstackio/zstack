//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import java.util.List;
import java.util.ArrayList;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class LogicalRouterPRListParams extends ApiPropertyBase {
    String logical_router_uuid;
    List<String> physical_router_uuid_list;
    public LogicalRouterPRListParams() {
    }
    public LogicalRouterPRListParams(String logical_router_uuid, List<String> physical_router_uuid_list) {
        this.logical_router_uuid = logical_router_uuid;
        this.physical_router_uuid_list = physical_router_uuid_list;
    }
    public LogicalRouterPRListParams(String logical_router_uuid) {
        this(logical_router_uuid, null);    }
    
    public String getLogicalRouterUuid() {
        return logical_router_uuid;
    }
    
    public void setLogicalRouterUuid(String logical_router_uuid) {
        this.logical_router_uuid = logical_router_uuid;
    }
    
    
    public List<String> getPhysicalRouterUuidList() {
        return physical_router_uuid_list;
    }
    
    
    public void addPhysicalRouterUuid(String obj) {
        if (physical_router_uuid_list == null) {
            physical_router_uuid_list = new ArrayList<String>();
        }
        physical_router_uuid_list.add(obj);
    }
    public void clearPhysicalRouterUuid() {
        physical_router_uuid_list = null;
    }
    
}
