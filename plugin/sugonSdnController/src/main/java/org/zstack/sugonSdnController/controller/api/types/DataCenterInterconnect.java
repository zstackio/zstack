//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import java.util.List;
import java.util.ArrayList;
import com.google.common.collect.Lists;

import org.zstack.sugonSdnController.controller.api.ApiObjectBase;
import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;
import org.zstack.sugonSdnController.controller.api.ObjectReference;

public class DataCenterInterconnect extends ApiObjectBase {
    private String data_center_interconnect_bgp_hold_time;
    private String data_center_interconnect_mode;
    private String data_center_interconnect_bgp_address_families;
    private RouteTargetList data_center_interconnect_configured_route_target_list;
    private String data_center_interconnect_type;
    private LogicalRouterPRListType destination_physical_router_list;
    private IdPermsType id_perms;
    private PermType2 perms2;
    private KeyValuePairs annotations;
    private String display_name;
    private List<ObjectReference<ApiPropertyBase>> logical_router_refs;
    private List<ObjectReference<ApiPropertyBase>> virtual_network_refs;
    private List<ObjectReference<ApiPropertyBase>> routing_policy_refs;
    private List<ObjectReference<ApiPropertyBase>> fabric_refs;
    private List<ObjectReference<ApiPropertyBase>> tag_refs;

    @Override
    public String getObjectType() {
        return "data-center-interconnect";
    }

    @Override
    public List<String> getDefaultParent() {
        return Lists.newArrayList("default-global-system-config");
    }

    @Override
    public String getDefaultParentType() {
        return "global-system-config";
    }

    public void setParent(GlobalSystemConfig parent) {
        super.setParent(parent);
    }
    
    public String getBgpHoldTime() {
        return data_center_interconnect_bgp_hold_time;
    }
    
    public void setBgpHoldTime(String data_center_interconnect_bgp_hold_time) {
        this.data_center_interconnect_bgp_hold_time = data_center_interconnect_bgp_hold_time;
    }
    
    
    public String getMode() {
        return data_center_interconnect_mode;
    }
    
    public void setMode(String data_center_interconnect_mode) {
        this.data_center_interconnect_mode = data_center_interconnect_mode;
    }
    
    
    public String getBgpAddressFamilies() {
        return data_center_interconnect_bgp_address_families;
    }
    
    public void setBgpAddressFamilies(String data_center_interconnect_bgp_address_families) {
        this.data_center_interconnect_bgp_address_families = data_center_interconnect_bgp_address_families;
    }
    
    
    public RouteTargetList getConfiguredRouteTargetList() {
        return data_center_interconnect_configured_route_target_list;
    }
    
    public void setConfiguredRouteTargetList(RouteTargetList data_center_interconnect_configured_route_target_list) {
        this.data_center_interconnect_configured_route_target_list = data_center_interconnect_configured_route_target_list;
    }
    
    
    public String getType() {
        return data_center_interconnect_type;
    }
    
    public void setType(String data_center_interconnect_type) {
        this.data_center_interconnect_type = data_center_interconnect_type;
    }
    
    
    public LogicalRouterPRListType getDestinationPhysicalRouterList() {
        return destination_physical_router_list;
    }
    
    public void setDestinationPhysicalRouterList(LogicalRouterPRListType destination_physical_router_list) {
        this.destination_physical_router_list = destination_physical_router_list;
    }
    
    
    public IdPermsType getIdPerms() {
        return id_perms;
    }
    
    public void setIdPerms(IdPermsType id_perms) {
        this.id_perms = id_perms;
    }
    
    
    public PermType2 getPerms2() {
        return perms2;
    }
    
    public void setPerms2(PermType2 perms2) {
        this.perms2 = perms2;
    }
    
    
    public KeyValuePairs getAnnotations() {
        return annotations;
    }
    
    public void setAnnotations(KeyValuePairs annotations) {
        this.annotations = annotations;
    }
    
    
    public String getDisplayName() {
        return display_name;
    }
    
    public void setDisplayName(String display_name) {
        this.display_name = display_name;
    }
    

    public List<ObjectReference<ApiPropertyBase>> getLogicalRouter() {
        return logical_router_refs;
    }

    public void setLogicalRouter(LogicalRouter obj) {
        logical_router_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        logical_router_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void addLogicalRouter(LogicalRouter obj) {
        if (logical_router_refs == null) {
            logical_router_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        }
        logical_router_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void removeLogicalRouter(LogicalRouter obj) {
        if (logical_router_refs != null) {
            logical_router_refs.remove(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
        }
    }

    public void clearLogicalRouter() {
        if (logical_router_refs != null) {
            logical_router_refs.clear();
            return;
        }
        logical_router_refs = null;
    }

    public List<ObjectReference<ApiPropertyBase>> getVirtualNetwork() {
        return virtual_network_refs;
    }

    public void setVirtualNetwork(VirtualNetwork obj) {
        virtual_network_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        virtual_network_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void addVirtualNetwork(VirtualNetwork obj) {
        if (virtual_network_refs == null) {
            virtual_network_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        }
        virtual_network_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void removeVirtualNetwork(VirtualNetwork obj) {
        if (virtual_network_refs != null) {
            virtual_network_refs.remove(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
        }
    }

    public void clearVirtualNetwork() {
        if (virtual_network_refs != null) {
            virtual_network_refs.clear();
            return;
        }
        virtual_network_refs = null;
    }

    public List<ObjectReference<ApiPropertyBase>> getRoutingPolicy() {
        return routing_policy_refs;
    }

    public void setRoutingPolicy(RoutingPolicy obj) {
        routing_policy_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        routing_policy_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void addRoutingPolicy(RoutingPolicy obj) {
        if (routing_policy_refs == null) {
            routing_policy_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        }
        routing_policy_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void removeRoutingPolicy(RoutingPolicy obj) {
        if (routing_policy_refs != null) {
            routing_policy_refs.remove(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
        }
    }

    public void clearRoutingPolicy() {
        if (routing_policy_refs != null) {
            routing_policy_refs.clear();
            return;
        }
        routing_policy_refs = null;
    }

    public List<ObjectReference<ApiPropertyBase>> getFabric() {
        return fabric_refs;
    }

    public void setFabric(Fabric obj) {
        fabric_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        fabric_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void addFabric(Fabric obj) {
        if (fabric_refs == null) {
            fabric_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        }
        fabric_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void removeFabric(Fabric obj) {
        if (fabric_refs != null) {
            fabric_refs.remove(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
        }
    }

    public void clearFabric() {
        if (fabric_refs != null) {
            fabric_refs.clear();
            return;
        }
        fabric_refs = null;
    }

    public List<ObjectReference<ApiPropertyBase>> getTag() {
        return tag_refs;
    }

    public void setTag(Tag obj) {
        tag_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        tag_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void addTag(Tag obj) {
        if (tag_refs == null) {
            tag_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        }
        tag_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void removeTag(Tag obj) {
        if (tag_refs != null) {
            tag_refs.remove(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
        }
    }

    public void clearTag() {
        if (tag_refs != null) {
            tag_refs.clear();
            return;
        }
        tag_refs = null;
    }
}