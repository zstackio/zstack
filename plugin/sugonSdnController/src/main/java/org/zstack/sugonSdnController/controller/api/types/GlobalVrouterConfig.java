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

public class GlobalVrouterConfig extends ApiObjectBase {
    private EcmpHashingIncludeFields ecmp_hashing_include_fields;
    private LinklocalServicesTypes linklocal_services;
    private EncapsulationPrioritiesType encapsulation_priorities;
    private String vxlan_network_identifier_mode;
    private Integer flow_export_rate;
    private FlowAgingTimeoutList flow_aging_timeout_list;
    private Boolean enable_security_logging;
    private String encryption_mode;
    private EncryptionTunnelEndpointList encryption_tunnel_endpoints;
    private String forwarding_mode;
    private PortTranslationPools port_translation_pools;
    private IdPermsType id_perms;
    private PermType2 perms2;
    private KeyValuePairs annotations;
    private String display_name;
    private List<ObjectReference<ApiPropertyBase>> security_logging_objects;
    private List<ObjectReference<ApiPropertyBase>> tag_refs;
    private transient List<ObjectReference<ApiPropertyBase>> application_policy_set_back_refs;

    @Override
    public String getObjectType() {
        return "global-vrouter-config";
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
    
    public EcmpHashingIncludeFields getEcmpHashingIncludeFields() {
        return ecmp_hashing_include_fields;
    }
    
    public void setEcmpHashingIncludeFields(EcmpHashingIncludeFields ecmp_hashing_include_fields) {
        this.ecmp_hashing_include_fields = ecmp_hashing_include_fields;
    }
    
    
    public LinklocalServicesTypes getLinklocalServices() {
        return linklocal_services;
    }
    
    public void setLinklocalServices(LinklocalServicesTypes linklocal_services) {
        this.linklocal_services = linklocal_services;
    }
    
    
    public EncapsulationPrioritiesType getEncapsulationPriorities() {
        return encapsulation_priorities;
    }
    
    public void setEncapsulationPriorities(EncapsulationPrioritiesType encapsulation_priorities) {
        this.encapsulation_priorities = encapsulation_priorities;
    }
    
    
    public String getVxlanNetworkIdentifierMode() {
        return vxlan_network_identifier_mode;
    }
    
    public void setVxlanNetworkIdentifierMode(String vxlan_network_identifier_mode) {
        this.vxlan_network_identifier_mode = vxlan_network_identifier_mode;
    }
    
    
    public Integer getFlowExportRate() {
        return flow_export_rate;
    }
    
    public void setFlowExportRate(Integer flow_export_rate) {
        this.flow_export_rate = flow_export_rate;
    }
    
    
    public FlowAgingTimeoutList getFlowAgingTimeoutList() {
        return flow_aging_timeout_list;
    }
    
    public void setFlowAgingTimeoutList(FlowAgingTimeoutList flow_aging_timeout_list) {
        this.flow_aging_timeout_list = flow_aging_timeout_list;
    }
    
    
    public Boolean getEnableSecurityLogging() {
        return enable_security_logging;
    }
    
    public void setEnableSecurityLogging(Boolean enable_security_logging) {
        this.enable_security_logging = enable_security_logging;
    }
    
    
    public String getEncryptionMode() {
        return encryption_mode;
    }
    
    public void setEncryptionMode(String encryption_mode) {
        this.encryption_mode = encryption_mode;
    }
    
    
    public EncryptionTunnelEndpointList getEncryptionTunnelEndpoints() {
        return encryption_tunnel_endpoints;
    }
    
    public void setEncryptionTunnelEndpoints(EncryptionTunnelEndpointList encryption_tunnel_endpoints) {
        this.encryption_tunnel_endpoints = encryption_tunnel_endpoints;
    }
    
    
    public String getForwardingMode() {
        return forwarding_mode;
    }
    
    public void setForwardingMode(String forwarding_mode) {
        this.forwarding_mode = forwarding_mode;
    }
    
    
    public PortTranslationPools getPortTranslationPools() {
        return port_translation_pools;
    }
    
    public void setPortTranslationPools(PortTranslationPools port_translation_pools) {
        this.port_translation_pools = port_translation_pools;
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
    

    public List<ObjectReference<ApiPropertyBase>> getSecurityLoggingObjects() {
        return security_logging_objects;
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

    public List<ObjectReference<ApiPropertyBase>> getApplicationPolicySetBackRefs() {
        return application_policy_set_back_refs;
    }
}