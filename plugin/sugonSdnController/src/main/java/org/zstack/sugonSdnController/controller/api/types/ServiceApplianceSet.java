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

public class ServiceApplianceSet extends ApiObjectBase {
    private String service_appliance_set_virtualization_type;
    private KeyValuePairs service_appliance_set_properties;
    private String service_appliance_driver;
    private String service_appliance_ha_mode;
    private IdPermsType id_perms;
    private PermType2 perms2;
    private KeyValuePairs annotations;
    private String display_name;
    private List<ObjectReference<ApiPropertyBase>> service_appliances;
    private List<ObjectReference<ApiPropertyBase>> tag_refs;
    private transient List<ObjectReference<ApiPropertyBase>> service_template_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> loadbalancer_pool_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> loadbalancer_back_refs;

    @Override
    public String getObjectType() {
        return "service-appliance-set";
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
    
    public String getVirtualizationType() {
        return service_appliance_set_virtualization_type;
    }
    
    public void setVirtualizationType(String service_appliance_set_virtualization_type) {
        this.service_appliance_set_virtualization_type = service_appliance_set_virtualization_type;
    }
    
    
    public KeyValuePairs getProperties() {
        return service_appliance_set_properties;
    }
    
    public void setProperties(KeyValuePairs service_appliance_set_properties) {
        this.service_appliance_set_properties = service_appliance_set_properties;
    }
    
    
    public String getServiceApplianceDriver() {
        return service_appliance_driver;
    }
    
    public void setServiceApplianceDriver(String service_appliance_driver) {
        this.service_appliance_driver = service_appliance_driver;
    }
    
    
    public String getServiceApplianceHaMode() {
        return service_appliance_ha_mode;
    }
    
    public void setServiceApplianceHaMode(String service_appliance_ha_mode) {
        this.service_appliance_ha_mode = service_appliance_ha_mode;
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
    

    public List<ObjectReference<ApiPropertyBase>> getServiceAppliances() {
        return service_appliances;
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

    public List<ObjectReference<ApiPropertyBase>> getServiceTemplateBackRefs() {
        return service_template_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getLoadbalancerPoolBackRefs() {
        return loadbalancer_pool_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getLoadbalancerBackRefs() {
        return loadbalancer_back_refs;
    }
}