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

public class Bgpvpn extends ApiObjectBase {
    private RouteTargetList route_target_list;
    private RouteTargetList import_route_target_list;
    private RouteTargetList export_route_target_list;
    private String bgpvpn_type;
    private IdPermsType id_perms;
    private PermType2 perms2;
    private KeyValuePairs annotations;
    private String display_name;
    private List<ObjectReference<ApiPropertyBase>> tag_refs;
    private transient List<ObjectReference<ApiPropertyBase>> virtual_network_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> logical_router_back_refs;

    @Override
    public String getObjectType() {
        return "bgpvpn";
    }

    @Override
    public List<String> getDefaultParent() {
        return Lists.newArrayList("default-domain", "default-project");
    }

    @Override
    public String getDefaultParentType() {
        return "project";
    }

    public void setParent(Project parent) {
        super.setParent(parent);
    }
    
    public RouteTargetList getRouteTargetList() {
        return route_target_list;
    }
    
    public void setRouteTargetList(RouteTargetList route_target_list) {
        this.route_target_list = route_target_list;
    }
    
    
    public RouteTargetList getImportRouteTargetList() {
        return import_route_target_list;
    }
    
    public void setImportRouteTargetList(RouteTargetList import_route_target_list) {
        this.import_route_target_list = import_route_target_list;
    }
    
    
    public RouteTargetList getExportRouteTargetList() {
        return export_route_target_list;
    }
    
    public void setExportRouteTargetList(RouteTargetList export_route_target_list) {
        this.export_route_target_list = export_route_target_list;
    }
    
    
    public String getType() {
        return bgpvpn_type;
    }
    
    public void setType(String bgpvpn_type) {
        this.bgpvpn_type = bgpvpn_type;
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

    public List<ObjectReference<ApiPropertyBase>> getVirtualNetworkBackRefs() {
        return virtual_network_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getLogicalRouterBackRefs() {
        return logical_router_back_refs;
    }
}