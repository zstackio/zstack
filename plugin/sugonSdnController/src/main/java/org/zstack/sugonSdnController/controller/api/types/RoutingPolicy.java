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

public class RoutingPolicy extends ApiObjectBase {
    private IdPermsType id_perms;
    private PermType2 perms2;
    private KeyValuePairs annotations;
    private String display_name;
    private List<ObjectReference<ApiPropertyBase>> interface_route_table_refs;
    private List<ObjectReference<RoutingPolicyServiceInstanceType>> service_instance_refs;
    private List<ObjectReference<ApiPropertyBase>> tag_refs;
    private transient List<ObjectReference<ApiPropertyBase>> data_center_interconnect_back_refs;

    @Override
    public String getObjectType() {
        return "routing-policy";
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
    

    public List<ObjectReference<ApiPropertyBase>> getInterfaceRouteTable() {
        return interface_route_table_refs;
    }

    public void setInterfaceRouteTable(InterfaceRouteTable obj) {
        interface_route_table_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        interface_route_table_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void addInterfaceRouteTable(InterfaceRouteTable obj) {
        if (interface_route_table_refs == null) {
            interface_route_table_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        }
        interface_route_table_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void removeInterfaceRouteTable(InterfaceRouteTable obj) {
        if (interface_route_table_refs != null) {
            interface_route_table_refs.remove(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
        }
    }

    public void clearInterfaceRouteTable() {
        if (interface_route_table_refs != null) {
            interface_route_table_refs.clear();
            return;
        }
        interface_route_table_refs = null;
    }

    public List<ObjectReference<RoutingPolicyServiceInstanceType>> getServiceInstance() {
        return service_instance_refs;
    }

    public void setServiceInstance(ServiceInstance obj, RoutingPolicyServiceInstanceType data) {
        service_instance_refs = new ArrayList<ObjectReference<RoutingPolicyServiceInstanceType>>();
        service_instance_refs.add(new ObjectReference<RoutingPolicyServiceInstanceType>(obj.getQualifiedName(), data));
    }

    public void addServiceInstance(ServiceInstance obj, RoutingPolicyServiceInstanceType data) {
        if (service_instance_refs == null) {
            service_instance_refs = new ArrayList<ObjectReference<RoutingPolicyServiceInstanceType>>();
        }
        service_instance_refs.add(new ObjectReference<RoutingPolicyServiceInstanceType>(obj.getQualifiedName(), data));
    }

    public void removeServiceInstance(ServiceInstance obj, RoutingPolicyServiceInstanceType data) {
        if (service_instance_refs != null) {
            service_instance_refs.remove(new ObjectReference<RoutingPolicyServiceInstanceType>(obj.getQualifiedName(), data));
        }
    }

    public void clearServiceInstance() {
        if (service_instance_refs != null) {
            service_instance_refs.clear();
            return;
        }
        service_instance_refs = null;
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

    public List<ObjectReference<ApiPropertyBase>> getDataCenterInterconnectBackRefs() {
        return data_center_interconnect_back_refs;
    }
}