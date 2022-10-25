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

public class TelemetryProfile extends ApiObjectBase {
    private Boolean telemetry_profile_is_default;
    private IdPermsType id_perms;
    private PermType2 perms2;
    private KeyValuePairs annotations;
    private String display_name;
    private List<ObjectReference<ApiPropertyBase>> sflow_profile_refs;
    private List<ObjectReference<ApiPropertyBase>> grpc_profile_refs;
    private List<ObjectReference<ApiPropertyBase>> netconf_profile_refs;
    private List<ObjectReference<ApiPropertyBase>> snmp_profile_refs;
    private List<ObjectReference<ApiPropertyBase>> tag_refs;
    private transient List<ObjectReference<ApiPropertyBase>> physical_router_back_refs;

    @Override
    public String getObjectType() {
        return "telemetry-profile";
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
    
    public Boolean getIsDefault() {
        return telemetry_profile_is_default;
    }
    
    public void setIsDefault(Boolean telemetry_profile_is_default) {
        this.telemetry_profile_is_default = telemetry_profile_is_default;
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
    

    public List<ObjectReference<ApiPropertyBase>> getSflowProfile() {
        return sflow_profile_refs;
    }

    public void setSflowProfile(SflowProfile obj) {
        sflow_profile_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        sflow_profile_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void addSflowProfile(SflowProfile obj) {
        if (sflow_profile_refs == null) {
            sflow_profile_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        }
        sflow_profile_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void removeSflowProfile(SflowProfile obj) {
        if (sflow_profile_refs != null) {
            sflow_profile_refs.remove(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
        }
    }

    public void clearSflowProfile() {
        if (sflow_profile_refs != null) {
            sflow_profile_refs.clear();
            return;
        }
        sflow_profile_refs = null;
    }

    public List<ObjectReference<ApiPropertyBase>> getGrpcProfile() {
        return grpc_profile_refs;
    }

    public void setGrpcProfile(GrpcProfile obj) {
        grpc_profile_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        grpc_profile_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void addGrpcProfile(GrpcProfile obj) {
        if (grpc_profile_refs == null) {
            grpc_profile_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        }
        grpc_profile_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void removeGrpcProfile(GrpcProfile obj) {
        if (grpc_profile_refs != null) {
            grpc_profile_refs.remove(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
        }
    }

    public void clearGrpcProfile() {
        if (grpc_profile_refs != null) {
            grpc_profile_refs.clear();
            return;
        }
        grpc_profile_refs = null;
    }

    public List<ObjectReference<ApiPropertyBase>> getNetconfProfile() {
        return netconf_profile_refs;
    }

    public void setNetconfProfile(NetconfProfile obj) {
        netconf_profile_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        netconf_profile_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void addNetconfProfile(NetconfProfile obj) {
        if (netconf_profile_refs == null) {
            netconf_profile_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        }
        netconf_profile_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void removeNetconfProfile(NetconfProfile obj) {
        if (netconf_profile_refs != null) {
            netconf_profile_refs.remove(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
        }
    }

    public void clearNetconfProfile() {
        if (netconf_profile_refs != null) {
            netconf_profile_refs.clear();
            return;
        }
        netconf_profile_refs = null;
    }

    public List<ObjectReference<ApiPropertyBase>> getSnmpProfile() {
        return snmp_profile_refs;
    }

    public void setSnmpProfile(SnmpProfile obj) {
        snmp_profile_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        snmp_profile_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void addSnmpProfile(SnmpProfile obj) {
        if (snmp_profile_refs == null) {
            snmp_profile_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        }
        snmp_profile_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void removeSnmpProfile(SnmpProfile obj) {
        if (snmp_profile_refs != null) {
            snmp_profile_refs.remove(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
        }
    }

    public void clearSnmpProfile() {
        if (snmp_profile_refs != null) {
            snmp_profile_refs.clear();
            return;
        }
        snmp_profile_refs = null;
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

    public List<ObjectReference<ApiPropertyBase>> getPhysicalRouterBackRefs() {
        return physical_router_back_refs;
    }
}