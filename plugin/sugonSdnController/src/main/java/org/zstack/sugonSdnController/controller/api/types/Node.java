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

public class Node extends ApiObjectBase {
    private String node_type;
    private ESXIHostInfo esxi_info;
    private String ip_address;
    private String hostname;
    private BaremetalServerInfo bms_info;
    private String mac_address;
    private String disk_partition;
    private String interface_name;
    private CloudInstanceInfo cloud_info;
    private IdPermsType id_perms;
    private PermType2 perms2;
    private KeyValuePairs annotations;
    private String display_name;
    private List<ObjectReference<ApiPropertyBase>> node_profile_refs;
    private List<ObjectReference<ApiPropertyBase>> ports;
    private List<ObjectReference<ApiPropertyBase>> port_groups;
    private List<ObjectReference<ApiPropertyBase>> tag_refs;

    @Override
    public String getObjectType() {
        return "node";
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
        return node_type;
    }
    
    public void setType(String node_type) {
        this.node_type = node_type;
    }
    
    
    public ESXIHostInfo getEsxiInfo() {
        return esxi_info;
    }
    
    public void setEsxiInfo(ESXIHostInfo esxi_info) {
        this.esxi_info = esxi_info;
    }
    
    
    public String getIpAddress() {
        return ip_address;
    }
    
    public void setIpAddress(String ip_address) {
        this.ip_address = ip_address;
    }
    
    
    public String getHostname() {
        return hostname;
    }
    
    public void setHostname(String hostname) {
        this.hostname = hostname;
    }
    
    
    public BaremetalServerInfo getBmsInfo() {
        return bms_info;
    }
    
    public void setBmsInfo(BaremetalServerInfo bms_info) {
        this.bms_info = bms_info;
    }
    
    
    public String getMacAddress() {
        return mac_address;
    }
    
    public void setMacAddress(String mac_address) {
        this.mac_address = mac_address;
    }
    
    
    public String getDiskPartition() {
        return disk_partition;
    }
    
    public void setDiskPartition(String disk_partition) {
        this.disk_partition = disk_partition;
    }
    
    
    public String getInterfaceName() {
        return interface_name;
    }
    
    public void setInterfaceName(String interface_name) {
        this.interface_name = interface_name;
    }
    
    
    public CloudInstanceInfo getCloudInfo() {
        return cloud_info;
    }
    
    public void setCloudInfo(CloudInstanceInfo cloud_info) {
        this.cloud_info = cloud_info;
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
    

    public List<ObjectReference<ApiPropertyBase>> getNodeProfile() {
        return node_profile_refs;
    }

    public void setNodeProfile(NodeProfile obj) {
        node_profile_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        node_profile_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void addNodeProfile(NodeProfile obj) {
        if (node_profile_refs == null) {
            node_profile_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        }
        node_profile_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void removeNodeProfile(NodeProfile obj) {
        if (node_profile_refs != null) {
            node_profile_refs.remove(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
        }
    }

    public void clearNodeProfile() {
        if (node_profile_refs != null) {
            node_profile_refs.clear();
            return;
        }
        node_profile_refs = null;
    }

    public List<ObjectReference<ApiPropertyBase>> getPorts() {
        return ports;
    }

    public List<ObjectReference<ApiPropertyBase>> getPortGroups() {
        return port_groups;
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
}