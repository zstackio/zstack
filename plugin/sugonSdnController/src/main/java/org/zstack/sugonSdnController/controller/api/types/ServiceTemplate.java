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

public class ServiceTemplate extends ApiObjectBase {
    private ServiceTemplateType service_template_properties;
    private Boolean service_config_managed;
    private IdPermsType id_perms;
    private PermType2 perms2;
    private KeyValuePairs annotations;
    private String display_name;
    private List<ObjectReference<ApiPropertyBase>> service_appliance_set_refs;
    private List<ObjectReference<ApiPropertyBase>> tag_refs;
    private transient List<ObjectReference<ApiPropertyBase>> service_instance_back_refs;

    @Override
    public String getObjectType() {
        return "service-template";
    }

    @Override
    public List<String> getDefaultParent() {
        return Lists.newArrayList("default-domain");
    }

    @Override
    public String getDefaultParentType() {
        return "domain";
    }

    public void setParent(Domain parent) {
        super.setParent(parent);
    }
    
    public ServiceTemplateType getProperties() {
        return service_template_properties;
    }
    
    public void setProperties(ServiceTemplateType service_template_properties) {
        this.service_template_properties = service_template_properties;
    }
    
    
    public Boolean getServiceConfigManaged() {
        return service_config_managed;
    }
    
    public void setServiceConfigManaged(Boolean service_config_managed) {
        this.service_config_managed = service_config_managed;
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
    

    public List<ObjectReference<ApiPropertyBase>> getServiceApplianceSet() {
        return service_appliance_set_refs;
    }

    public void setServiceApplianceSet(ServiceApplianceSet obj) {
        service_appliance_set_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        service_appliance_set_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void addServiceApplianceSet(ServiceApplianceSet obj) {
        if (service_appliance_set_refs == null) {
            service_appliance_set_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        }
        service_appliance_set_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void removeServiceApplianceSet(ServiceApplianceSet obj) {
        if (service_appliance_set_refs != null) {
            service_appliance_set_refs.remove(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
        }
    }

    public void clearServiceApplianceSet() {
        if (service_appliance_set_refs != null) {
            service_appliance_set_refs.clear();
            return;
        }
        service_appliance_set_refs = null;
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

    public List<ObjectReference<ApiPropertyBase>> getServiceInstanceBackRefs() {
        return service_instance_back_refs;
    }
}