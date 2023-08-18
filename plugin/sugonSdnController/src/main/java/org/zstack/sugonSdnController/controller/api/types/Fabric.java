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

public class Fabric extends ApiObjectBase {
    private Boolean fabric_ztp;
    private String fabric_os_version;
    private DeviceCredentialList fabric_credentials;
    private Boolean fabric_enterprise_style;
    private Boolean disable_vlan_vn_uniqueness_check;
    private IdPermsType id_perms;
    private PermType2 perms2;
    private KeyValuePairs annotations;
    private String display_name;
    private List<ObjectReference<ApiPropertyBase>> intent_map_refs;
    private List<ObjectReference<FabricNetworkTag>> virtual_network_refs;
    private List<ObjectReference<ApiPropertyBase>> fabric_namespaces;
    private List<ObjectReference<SerialNumListType>> node_profile_refs;
    private List<ObjectReference<ApiPropertyBase>> virtual_port_groups;
    private List<ObjectReference<ApiPropertyBase>> tag_refs;
    private transient List<ObjectReference<ApiPropertyBase>> logical_router_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> data_center_interconnect_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> physical_router_back_refs;

    @Override
    public String getObjectType() {
        return "fabric";
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
    
    public Boolean getZtp() {
        return fabric_ztp;
    }
    
    public void setZtp(Boolean fabric_ztp) {
        this.fabric_ztp = fabric_ztp;
    }
    
    
    public String getOsVersion() {
        return fabric_os_version;
    }
    
    public void setOsVersion(String fabric_os_version) {
        this.fabric_os_version = fabric_os_version;
    }
    
    
    public DeviceCredentialList getCredentials() {
        return fabric_credentials;
    }
    
    public void setCredentials(DeviceCredentialList fabric_credentials) {
        this.fabric_credentials = fabric_credentials;
    }
    
    
    public Boolean getEnterpriseStyle() {
        return fabric_enterprise_style;
    }
    
    public void setEnterpriseStyle(Boolean fabric_enterprise_style) {
        this.fabric_enterprise_style = fabric_enterprise_style;
    }
    
    
    public Boolean getDisableVlanVnUniquenessCheck() {
        return disable_vlan_vn_uniqueness_check;
    }
    
    public void setDisableVlanVnUniquenessCheck(Boolean disable_vlan_vn_uniqueness_check) {
        this.disable_vlan_vn_uniqueness_check = disable_vlan_vn_uniqueness_check;
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
    

    public List<ObjectReference<ApiPropertyBase>> getIntentMap() {
        return intent_map_refs;
    }

    public void setIntentMap(IntentMap obj) {
        intent_map_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        intent_map_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void addIntentMap(IntentMap obj) {
        if (intent_map_refs == null) {
            intent_map_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        }
        intent_map_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void removeIntentMap(IntentMap obj) {
        if (intent_map_refs != null) {
            intent_map_refs.remove(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
        }
    }

    public void clearIntentMap() {
        if (intent_map_refs != null) {
            intent_map_refs.clear();
            return;
        }
        intent_map_refs = null;
    }

    public List<ObjectReference<FabricNetworkTag>> getVirtualNetwork() {
        return virtual_network_refs;
    }

    public void setVirtualNetwork(VirtualNetwork obj, FabricNetworkTag data) {
        virtual_network_refs = new ArrayList<ObjectReference<FabricNetworkTag>>();
        virtual_network_refs.add(new ObjectReference<FabricNetworkTag>(obj.getQualifiedName(), data));
    }

    public void addVirtualNetwork(VirtualNetwork obj, FabricNetworkTag data) {
        if (virtual_network_refs == null) {
            virtual_network_refs = new ArrayList<ObjectReference<FabricNetworkTag>>();
        }
        virtual_network_refs.add(new ObjectReference<FabricNetworkTag>(obj.getQualifiedName(), data));
    }

    public void removeVirtualNetwork(VirtualNetwork obj, FabricNetworkTag data) {
        if (virtual_network_refs != null) {
            virtual_network_refs.remove(new ObjectReference<FabricNetworkTag>(obj.getQualifiedName(), data));
        }
    }

    public void clearVirtualNetwork() {
        if (virtual_network_refs != null) {
            virtual_network_refs.clear();
            return;
        }
        virtual_network_refs = null;
    }


    public List<ObjectReference<ApiPropertyBase>> getFabricNamespaces() {
        return fabric_namespaces;
    }

    public List<ObjectReference<SerialNumListType>> getNodeProfile() {
        return node_profile_refs;
    }

    public void setNodeProfile(NodeProfile obj, SerialNumListType data) {
        node_profile_refs = new ArrayList<ObjectReference<SerialNumListType>>();
        node_profile_refs.add(new ObjectReference<SerialNumListType>(obj.getQualifiedName(), data));
    }

    public void addNodeProfile(NodeProfile obj, SerialNumListType data) {
        if (node_profile_refs == null) {
            node_profile_refs = new ArrayList<ObjectReference<SerialNumListType>>();
        }
        node_profile_refs.add(new ObjectReference<SerialNumListType>(obj.getQualifiedName(), data));
    }

    public void removeNodeProfile(NodeProfile obj, SerialNumListType data) {
        if (node_profile_refs != null) {
            node_profile_refs.remove(new ObjectReference<SerialNumListType>(obj.getQualifiedName(), data));
        }
    }

    public void clearNodeProfile() {
        if (node_profile_refs != null) {
            node_profile_refs.clear();
            return;
        }
        node_profile_refs = null;
    }


    public List<ObjectReference<ApiPropertyBase>> getVirtualPortGroups() {
        return virtual_port_groups;
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

    public List<ObjectReference<ApiPropertyBase>> getLogicalRouterBackRefs() {
        return logical_router_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getDataCenterInterconnectBackRefs() {
        return data_center_interconnect_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getPhysicalRouterBackRefs() {
        return physical_router_back_refs;
    }
}