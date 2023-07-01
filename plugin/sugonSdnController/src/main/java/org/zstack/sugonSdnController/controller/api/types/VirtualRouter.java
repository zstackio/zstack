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

public class VirtualRouter extends ApiObjectBase {
    private String virtual_router_type;
    private Boolean virtual_router_dpdk_enabled;
    private KeyValuePairs virtual_router_sriov_physical_networks;
    private String virtual_router_ip_address;
    private IdPermsType id_perms;
    private PermType2 perms2;
    private KeyValuePairs annotations;
    private String display_name;
    private List<ObjectReference<VirtualRouterNetworkIpamType>> network_ipam_refs;
    private List<ObjectReference<ApiPropertyBase>> virtual_machine_interfaces;
    private List<ObjectReference<ApiPropertyBase>> sub_cluster_refs;
    private List<ObjectReference<ApiPropertyBase>> virtual_machine_refs;
    private List<ObjectReference<ApiPropertyBase>> tag_refs;
    private transient List<ObjectReference<ApiPropertyBase>> instance_ip_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> physical_router_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> provider_attachment_back_refs;

    @Override
    public String getObjectType() {
        return "virtual-router";
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
    
    public String getType() {
        return virtual_router_type;
    }
    
    public void setType(String virtual_router_type) {
        this.virtual_router_type = virtual_router_type;
    }
    
    
    public Boolean getDpdkEnabled() {
        return virtual_router_dpdk_enabled;
    }
    
    public void setDpdkEnabled(Boolean virtual_router_dpdk_enabled) {
        this.virtual_router_dpdk_enabled = virtual_router_dpdk_enabled;
    }
    
    
    public KeyValuePairs getSriovPhysicalNetworks() {
        return virtual_router_sriov_physical_networks;
    }
    
    public void setSriovPhysicalNetworks(KeyValuePairs virtual_router_sriov_physical_networks) {
        this.virtual_router_sriov_physical_networks = virtual_router_sriov_physical_networks;
    }
    
    
    public String getIpAddress() {
        return virtual_router_ip_address;
    }
    
    public void setIpAddress(String virtual_router_ip_address) {
        this.virtual_router_ip_address = virtual_router_ip_address;
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
    

    public List<ObjectReference<VirtualRouterNetworkIpamType>> getNetworkIpam() {
        return network_ipam_refs;
    }

    public void setNetworkIpam(NetworkIpam obj, VirtualRouterNetworkIpamType data) {
        network_ipam_refs = new ArrayList<ObjectReference<VirtualRouterNetworkIpamType>>();
        network_ipam_refs.add(new ObjectReference<VirtualRouterNetworkIpamType>(obj.getQualifiedName(), data));
    }

    public void addNetworkIpam(NetworkIpam obj, VirtualRouterNetworkIpamType data) {
        if (network_ipam_refs == null) {
            network_ipam_refs = new ArrayList<ObjectReference<VirtualRouterNetworkIpamType>>();
        }
        network_ipam_refs.add(new ObjectReference<VirtualRouterNetworkIpamType>(obj.getQualifiedName(), data));
    }

    public void removeNetworkIpam(NetworkIpam obj, VirtualRouterNetworkIpamType data) {
        if (network_ipam_refs != null) {
            network_ipam_refs.remove(new ObjectReference<VirtualRouterNetworkIpamType>(obj.getQualifiedName(), data));
        }
    }

    public void clearNetworkIpam() {
        if (network_ipam_refs != null) {
            network_ipam_refs.clear();
            return;
        }
        network_ipam_refs = null;
    }


    public List<ObjectReference<ApiPropertyBase>> getVirtualMachineInterfaces() {
        return virtual_machine_interfaces;
    }

    public List<ObjectReference<ApiPropertyBase>> getSubCluster() {
        return sub_cluster_refs;
    }

    public void setSubCluster(SubCluster obj) {
        sub_cluster_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        sub_cluster_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void addSubCluster(SubCluster obj) {
        if (sub_cluster_refs == null) {
            sub_cluster_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        }
        sub_cluster_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void removeSubCluster(SubCluster obj) {
        if (sub_cluster_refs != null) {
            sub_cluster_refs.remove(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
        }
    }

    public void clearSubCluster() {
        if (sub_cluster_refs != null) {
            sub_cluster_refs.clear();
            return;
        }
        sub_cluster_refs = null;
    }

    public List<ObjectReference<ApiPropertyBase>> getVirtualMachine() {
        return virtual_machine_refs;
    }

    public void setVirtualMachine(VirtualMachine obj) {
        virtual_machine_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        virtual_machine_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void addVirtualMachine(VirtualMachine obj) {
        if (virtual_machine_refs == null) {
            virtual_machine_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        }
        virtual_machine_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void removeVirtualMachine(VirtualMachine obj) {
        if (virtual_machine_refs != null) {
            virtual_machine_refs.remove(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
        }
    }

    public void clearVirtualMachine() {
        if (virtual_machine_refs != null) {
            virtual_machine_refs.clear();
            return;
        }
        virtual_machine_refs = null;
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

    public List<ObjectReference<ApiPropertyBase>> getInstanceIpBackRefs() {
        return instance_ip_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getPhysicalRouterBackRefs() {
        return physical_router_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getProviderAttachmentBackRefs() {
        return provider_attachment_back_refs;
    }
}