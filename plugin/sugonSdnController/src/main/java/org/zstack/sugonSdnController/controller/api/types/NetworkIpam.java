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

public class NetworkIpam extends ApiObjectBase {
    private IpamType network_ipam_mgmt;
    private IpamSubnets ipam_subnets;
    private String ipam_subnet_method;
    private Boolean ipam_subnetting;
    private IdPermsType id_perms;
    private PermType2 perms2;
    private KeyValuePairs annotations;
    private String display_name;
    private List<ObjectReference<ApiPropertyBase>> virtual_DNS_refs;
    private List<ObjectReference<ApiPropertyBase>> tag_refs;
    private transient List<ObjectReference<VnSubnetsType>> virtual_network_back_refs;
    private transient List<ObjectReference<VirtualRouterNetworkIpamType>> virtual_router_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> instance_ip_back_refs;

    @Override
    public String getObjectType() {
        return "network-ipam";
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
    
    public IpamType getMgmt() {
        return network_ipam_mgmt;
    }
    
    public void setMgmt(IpamType network_ipam_mgmt) {
        this.network_ipam_mgmt = network_ipam_mgmt;
    }
    
    
    public IpamSubnets getIpamSubnets() {
        return ipam_subnets;
    }
    
    public void setIpamSubnets(IpamSubnets ipam_subnets) {
        this.ipam_subnets = ipam_subnets;
    }
    
    
    public String getIpamSubnetMethod() {
        return ipam_subnet_method;
    }
    
    public void setIpamSubnetMethod(String ipam_subnet_method) {
        this.ipam_subnet_method = ipam_subnet_method;
    }
    
    
    public Boolean getIpamSubnetting() {
        return ipam_subnetting;
    }
    
    public void setIpamSubnetting(Boolean ipam_subnetting) {
        this.ipam_subnetting = ipam_subnetting;
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
    

    public List<ObjectReference<ApiPropertyBase>> getVirtualDns() {
        return virtual_DNS_refs;
    }

    public void setVirtualDns(VirtualDns obj) {
        virtual_DNS_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        virtual_DNS_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void addVirtualDns(VirtualDns obj) {
        if (virtual_DNS_refs == null) {
            virtual_DNS_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        }
        virtual_DNS_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void removeVirtualDns(VirtualDns obj) {
        if (virtual_DNS_refs != null) {
            virtual_DNS_refs.remove(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
        }
    }

    public void clearVirtualDns() {
        if (virtual_DNS_refs != null) {
            virtual_DNS_refs.clear();
            return;
        }
        virtual_DNS_refs = null;
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

    public List<ObjectReference<VnSubnetsType>> getVirtualNetworkBackRefs() {
        return virtual_network_back_refs;
    }

    public List<ObjectReference<VirtualRouterNetworkIpamType>> getVirtualRouterBackRefs() {
        return virtual_router_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getInstanceIpBackRefs() {
        return instance_ip_back_refs;
    }
}