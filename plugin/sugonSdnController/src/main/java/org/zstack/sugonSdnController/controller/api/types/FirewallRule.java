//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import java.util.List;
import java.util.ArrayList;

import org.zstack.sugonSdnController.controller.api.ApiObjectBase;
import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;
import org.zstack.sugonSdnController.controller.api.ObjectReference;

public class FirewallRule extends ApiObjectBase {
    private String draft_mode_state;
    private ActionListType action_list;
    private FirewallServiceType service;
    private FirewallRuleEndpointType endpoint_1;
    private FirewallRuleEndpointType endpoint_2;
    private FirewallRuleMatchTagsType match_tags;
    private FirewallRuleMatchTagsTypeIdList match_tag_types;
    private String direction;
    private IdPermsType id_perms;
    private PermType2 perms2;
    private KeyValuePairs annotations;
    private String display_name;
    private List<ObjectReference<ApiPropertyBase>> service_group_refs;
    private List<ObjectReference<ApiPropertyBase>> address_group_refs;
    private List<ObjectReference<ApiPropertyBase>> virtual_network_refs;
    private List<ObjectReference<SloRateType>> security_logging_object_refs;
    private List<ObjectReference<ApiPropertyBase>> tag_refs;
    private transient List<ObjectReference<FirewallSequence>> firewall_policy_back_refs;

    @Override
    public String getObjectType() {
        return "firewall-rule";
    }

    @Override
    public List<String> getDefaultParent() {
        return null;
    }

    @Override
    public String getDefaultParentType() {
        return null;
    }

    public void setParent(PolicyManagement parent) {
        super.setParent(parent);
    }

    public void setParent(Project parent) {
        super.setParent(parent);
    }
    
    public String getDraftModeState() {
        return draft_mode_state;
    }
    
    public void setDraftModeState(String draft_mode_state) {
        this.draft_mode_state = draft_mode_state;
    }
    
    
    public ActionListType getActionList() {
        return action_list;
    }
    
    public void setActionList(ActionListType action_list) {
        this.action_list = action_list;
    }
    
    
    public FirewallServiceType getService() {
        return service;
    }
    
    public void setService(FirewallServiceType service) {
        this.service = service;
    }
    
    
    public FirewallRuleEndpointType getEndpoint1() {
        return endpoint_1;
    }
    
    public void setEndpoint1(FirewallRuleEndpointType endpoint_1) {
        this.endpoint_1 = endpoint_1;
    }
    
    
    public FirewallRuleEndpointType getEndpoint2() {
        return endpoint_2;
    }
    
    public void setEndpoint2(FirewallRuleEndpointType endpoint_2) {
        this.endpoint_2 = endpoint_2;
    }
    
    
    public FirewallRuleMatchTagsType getMatchTags() {
        return match_tags;
    }
    
    public void setMatchTags(FirewallRuleMatchTagsType match_tags) {
        this.match_tags = match_tags;
    }
    
    
    public FirewallRuleMatchTagsTypeIdList getMatchTagTypes() {
        return match_tag_types;
    }
    
    public void setMatchTagTypes(FirewallRuleMatchTagsTypeIdList match_tag_types) {
        this.match_tag_types = match_tag_types;
    }
    
    
    public String getDirection() {
        return direction;
    }
    
    public void setDirection(String direction) {
        this.direction = direction;
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
    

    public List<ObjectReference<ApiPropertyBase>> getServiceGroup() {
        return service_group_refs;
    }

    public void setServiceGroup(ServiceGroup obj) {
        service_group_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        service_group_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void addServiceGroup(ServiceGroup obj) {
        if (service_group_refs == null) {
            service_group_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        }
        service_group_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void removeServiceGroup(ServiceGroup obj) {
        if (service_group_refs != null) {
            service_group_refs.remove(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
        }
    }

    public void clearServiceGroup() {
        if (service_group_refs != null) {
            service_group_refs.clear();
            return;
        }
        service_group_refs = null;
    }

    public List<ObjectReference<ApiPropertyBase>> getAddressGroup() {
        return address_group_refs;
    }

    public void setAddressGroup(AddressGroup obj) {
        address_group_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        address_group_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void addAddressGroup(AddressGroup obj) {
        if (address_group_refs == null) {
            address_group_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        }
        address_group_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void removeAddressGroup(AddressGroup obj) {
        if (address_group_refs != null) {
            address_group_refs.remove(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
        }
    }

    public void clearAddressGroup() {
        if (address_group_refs != null) {
            address_group_refs.clear();
            return;
        }
        address_group_refs = null;
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

    public List<ObjectReference<SloRateType>> getSecurityLoggingObject() {
        return security_logging_object_refs;
    }

    public void setSecurityLoggingObject(SecurityLoggingObject obj, SloRateType data) {
        security_logging_object_refs = new ArrayList<ObjectReference<SloRateType>>();
        security_logging_object_refs.add(new ObjectReference<SloRateType>(obj.getQualifiedName(), data));
    }

    public void addSecurityLoggingObject(SecurityLoggingObject obj, SloRateType data) {
        if (security_logging_object_refs == null) {
            security_logging_object_refs = new ArrayList<ObjectReference<SloRateType>>();
        }
        security_logging_object_refs.add(new ObjectReference<SloRateType>(obj.getQualifiedName(), data));
    }

    public void removeSecurityLoggingObject(SecurityLoggingObject obj, SloRateType data) {
        if (security_logging_object_refs != null) {
            security_logging_object_refs.remove(new ObjectReference<SloRateType>(obj.getQualifiedName(), data));
        }
    }

    public void clearSecurityLoggingObject() {
        if (security_logging_object_refs != null) {
            security_logging_object_refs.clear();
            return;
        }
        security_logging_object_refs = null;
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

    public List<ObjectReference<FirewallSequence>> getFirewallPolicyBackRefs() {
        return firewall_policy_back_refs;
    }
}