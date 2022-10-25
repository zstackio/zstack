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

public class ServiceInstance extends ApiObjectBase {
    private ServiceInstanceType service_instance_properties;
    private KeyValuePairs service_instance_bindings;
    private Boolean service_instance_bgp_enabled;
    private IdPermsType id_perms;
    private PermType2 perms2;
    private KeyValuePairs annotations;
    private String display_name;
    private List<ObjectReference<ApiPropertyBase>> service_template_refs;
    private List<ObjectReference<ServiceInterfaceTag>> instance_ip_refs;
    private List<ObjectReference<ApiPropertyBase>> port_tuples;
    private List<ObjectReference<ApiPropertyBase>> tag_refs;
    private transient List<ObjectReference<ApiPropertyBase>> virtual_machine_back_refs;
    private transient List<ObjectReference<ServiceInterfaceTag>> service_health_check_back_refs;
    private transient List<ObjectReference<ServiceInterfaceTag>> interface_route_table_back_refs;
    private transient List<ObjectReference<RoutingPolicyServiceInstanceType>> routing_policy_back_refs;
    private transient List<ObjectReference<ServiceInterfaceTag>> route_aggregate_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> logical_router_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> loadbalancer_pool_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> loadbalancer_back_refs;

    @Override
    public String getObjectType() {
        return "service-instance";
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
    
    public ServiceInstanceType getProperties() {
        return service_instance_properties;
    }
    
    public void setProperties(ServiceInstanceType service_instance_properties) {
        this.service_instance_properties = service_instance_properties;
    }
    
    
    public KeyValuePairs getBindings() {
        return service_instance_bindings;
    }
    
    public void setBindings(KeyValuePairs service_instance_bindings) {
        this.service_instance_bindings = service_instance_bindings;
    }
    
    
    public Boolean getBgpEnabled() {
        return service_instance_bgp_enabled;
    }
    
    public void setBgpEnabled(Boolean service_instance_bgp_enabled) {
        this.service_instance_bgp_enabled = service_instance_bgp_enabled;
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
    

    public List<ObjectReference<ApiPropertyBase>> getServiceTemplate() {
        return service_template_refs;
    }

    public void setServiceTemplate(ServiceTemplate obj) {
        service_template_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        service_template_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void addServiceTemplate(ServiceTemplate obj) {
        if (service_template_refs == null) {
            service_template_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        }
        service_template_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void removeServiceTemplate(ServiceTemplate obj) {
        if (service_template_refs != null) {
            service_template_refs.remove(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
        }
    }

    public void clearServiceTemplate() {
        if (service_template_refs != null) {
            service_template_refs.clear();
            return;
        }
        service_template_refs = null;
    }

    public List<ObjectReference<ServiceInterfaceTag>> getInstanceIp() {
        return instance_ip_refs;
    }

    public void setInstanceIp(InstanceIp obj, ServiceInterfaceTag data) {
        instance_ip_refs = new ArrayList<ObjectReference<ServiceInterfaceTag>>();
        instance_ip_refs.add(new ObjectReference<ServiceInterfaceTag>(obj.getQualifiedName(), data));
    }

    public void addInstanceIp(InstanceIp obj, ServiceInterfaceTag data) {
        if (instance_ip_refs == null) {
            instance_ip_refs = new ArrayList<ObjectReference<ServiceInterfaceTag>>();
        }
        instance_ip_refs.add(new ObjectReference<ServiceInterfaceTag>(obj.getQualifiedName(), data));
    }

    public void removeInstanceIp(InstanceIp obj, ServiceInterfaceTag data) {
        if (instance_ip_refs != null) {
            instance_ip_refs.remove(new ObjectReference<ServiceInterfaceTag>(obj.getQualifiedName(), data));
        }
    }

    public void clearInstanceIp() {
        if (instance_ip_refs != null) {
            instance_ip_refs.clear();
            return;
        }
        instance_ip_refs = null;
    }


    public List<ObjectReference<ApiPropertyBase>> getPortTuples() {
        return port_tuples;
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

    public List<ObjectReference<ApiPropertyBase>> getVirtualMachineBackRefs() {
        return virtual_machine_back_refs;
    }

    public List<ObjectReference<ServiceInterfaceTag>> getServiceHealthCheckBackRefs() {
        return service_health_check_back_refs;
    }

    public List<ObjectReference<ServiceInterfaceTag>> getInterfaceRouteTableBackRefs() {
        return interface_route_table_back_refs;
    }

    public List<ObjectReference<RoutingPolicyServiceInstanceType>> getRoutingPolicyBackRefs() {
        return routing_policy_back_refs;
    }

    public List<ObjectReference<ServiceInterfaceTag>> getRouteAggregateBackRefs() {
        return route_aggregate_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getLogicalRouterBackRefs() {
        return logical_router_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getLoadbalancerPoolBackRefs() {
        return loadbalancer_pool_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getLoadbalancerBackRefs() {
        return loadbalancer_back_refs;
    }
}